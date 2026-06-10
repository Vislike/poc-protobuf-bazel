package poc.server.thread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import poc.protocol.Chat.Message;
import poc.server.JavaServerMain;
import poc.server.event.IEvent;

public class RemoteClient implements AutoCloseable {

    private final SocketChannel channel;
    private final BlockingQueue<IEvent> mainQueue;
    private final BlockingQueue<IEvent> clientQueue;
    private final Thread receiveThread;
    private final Thread sendThread;
    private final AtomicBoolean run = new AtomicBoolean(true);

    public RemoteClient(SocketChannel channel, BlockingQueue<IEvent> mainQueue) {
        this.channel = channel;
        this.mainQueue = mainQueue;
        clientQueue = new ArrayBlockingQueue<>(JavaServerMain.CLIENT_MESSAGE_QUEUE_SIZE);
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
                    channel.read(lengthBB);
                }

                // Receive message
                int length = Short.toUnsignedInt(lengthBB.rewind().getShort());
                System.out.println("rec:" + length);
                ByteBuffer msgBB = ByteBuffer.allocate(length);
                while (msgBB.hasRemaining()) {
                    channel.read(msgBB);
                }

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

    @Override
    public void close() throws InterruptedException, IOException {
        run.setOpaque(false);
        receiveThread.interrupt();
        receiveThread.join();
        sendThread.interrupt();
        sendThread.join();
        channel.close();
    }
}
