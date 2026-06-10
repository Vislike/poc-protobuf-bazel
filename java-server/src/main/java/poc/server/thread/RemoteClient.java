package poc.server.thread;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import poc.protocol.Chat.Message;
import poc.server.JavaServerMain;
import poc.server.event.IEvent;
import poc.server.util.Utils;

public class RemoteClient implements AutoCloseable {

    private final String remoteAddress;
    private final SocketChannel channel;
    private final BlockingQueue<IEvent> mainQueue;
    private final BlockingQueue<IEvent> clientQueue;
    private final AtomicLong lastMessage;
    private final AtomicBoolean run;

    private Thread receiveThread;
    private Thread sendThread;

    public RemoteClient(SocketChannel channel, BlockingQueue<IEvent> mainQueue) throws IOException {
        this.remoteAddress = channel.getRemoteAddress().toString();
        this.channel = channel;
        this.mainQueue = mainQueue;
        this.clientQueue = new ArrayBlockingQueue<>(JavaServerMain.CLIENT_MESSAGE_QUEUE_SIZE);
        this.lastMessage = new AtomicLong(Utils.tickMs());
        this.run = new AtomicBoolean(true);
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
                lengthBB.clear();
                while (lengthBB.hasRemaining()) {
                    read(lengthBB);
                }

                // Receive message
                int length = Short.toUnsignedInt(lengthBB.rewind().getShort());
                ByteBuffer msgBB = ByteBuffer.allocate(length);
                while (msgBB.hasRemaining()) {
                    read(msgBB);
                }

                // Update activity
                lastMessage.setOpaque(Utils.tickMs());

                // Decode
                Message message = Message.parseFrom(msgBB.rewind());
                System.out.println(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendThread() {

    }

    private void read(ByteBuffer bb) throws IOException {
        if (channel.read(bb) == -1) {
            throw new EOFException();
        }
    }

    @Override
    public void close() throws InterruptedException, IOException {
        try {
            run.setOpaque(false);
            receiveThread.interrupt();
            receiveThread.join();
            sendThread.interrupt();
            sendThread.join();
            channel.close();
        } finally {
            System.out.println("Client disconnected: " + this);
        }
    }

    @Override
    public String toString() {
        return remoteAddress;
    }
}
