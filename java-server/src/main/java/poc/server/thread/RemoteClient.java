package poc.server.thread;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import poc.protocol.Chat.Message;
import poc.server.JavaServerMain;
import poc.server.event.IEvent;
import poc.server.event.IEvent.ClientRemoveEvent;
import poc.server.event.IEvent.UserIncomingEvent;
import poc.server.event.IEvent.UserOutgoingEvent;
import poc.server.util.Utils;

public class RemoteClient implements AutoCloseable {

    // Internal states of running Threads
    private final String remoteAddress;
    private final SocketChannel channel;
    private final BlockingQueue<IEvent> mainQueue;
    private final AtomicLong lastMessage;
    private final AtomicBoolean run = new AtomicBoolean(true);

    private Thread receiveThread;
    private Thread sendThread;

    // Owned by main event thread
    public final BlockingQueue<IEvent> clientQueue;
    public String userName;
    public boolean active;

    public RemoteClient(SocketChannel channel, BlockingQueue<IEvent> mainQueue) throws IOException {
        this.remoteAddress = channel.getRemoteAddress().toString();
        this.channel = channel;
        this.mainQueue = mainQueue;
        this.lastMessage = new AtomicLong(Utils.tickMs());
        this.clientQueue = new ArrayBlockingQueue<>(JavaServerMain.CLIENT_MESSAGE_QUEUE_SIZE);
    }

    public void start() {
        receiveThread = Thread.ofVirtual().start(this::receiveThread);
        sendThread = Thread.ofVirtual().start(this::sendThread);
    }

    private void receiveThread() {
        try {
            ByteBuffer lengthBB = ByteBuffer.allocate(2);
            while (run.getOpaque()) {
                // Receive message length
                socketReadUntilEOF(lengthBB.clear());

                // Receive message
                ByteBuffer msgBB = ByteBuffer.allocate(Short.toUnsignedInt(lengthBB.rewind().getShort()));
                socketReadUntilEOF(msgBB);

                // Update activity
                lastMessage.setOpaque(Utils.tickMs());

                // Decode & Send to main event thread, ignore pong
                Message message = Message.parseFrom(msgBB.rewind());
                if (!message.hasPong()) {
                    mainQueue.put(new UserIncomingEvent(this, message));
                }
            }
        } catch (Exception e) {
            if (run.getOpaque()) {
                switch (e) {
                    case EOFException _ -> mainQueue.offer(new ClientRemoveEvent(this, "Client hung up"));
                    case IOException _ -> mainQueue.offer(new ClientRemoveEvent(this, e.getMessage()));
                    default -> throw new AssertionError(e);
                }
            }
        }
    }

    private void sendThread() {
        try {
            ByteBuffer lengthBB = ByteBuffer.allocate(2);
            while (run.getOpaque()) {
                IEvent event = clientQueue.take();
                switch (event) {
                    case UserOutgoingEvent e -> socketSend(lengthBB, e.sharedBB().asReadOnlyBuffer());
                    default -> System.err.println("Unknown event: " + event);
                }
            }
        } catch (Exception e) {
            if (run.getOpaque()) {
                switch (e) {
                    case IOException _ -> mainQueue.offer(new ClientRemoveEvent(this, e.getMessage()));
                    default -> throw new AssertionError(e);
                }
            }
        }
    }

    private void socketSend(ByteBuffer lengthBB, ByteBuffer bb) throws IOException {
        lengthBB.clear().putShort((short) bb.capacity());
        channel.write(lengthBB.rewind());
        channel.write(bb);
    }

    private void socketReadUntilEOF(ByteBuffer bb) throws IOException {
        while (bb.hasRemaining()) {
            if (channel.read(bb) == -1) {
                throw new EOFException();
            }
        }
    }

    public boolean ageSeconds(int s, long tickMs) {
        return lastMessage.getOpaque() + TimeUnit.SECONDS.toMillis(s) < tickMs;
    }

    @Override
    public void close() throws InterruptedException, IOException {
        try {
            run.setOpaque(false);
            receiveThread.interrupt();
            sendThread.interrupt();
            receiveThread.join();
            sendThread.join();
            channel.close();
        } finally {
            System.out.println("Client disconnected: " + this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(remoteAddress);
        if (userName != null) {
            sb.append(" (").append(userName).append(")");
        }
        return sb.toString();
    }
}
