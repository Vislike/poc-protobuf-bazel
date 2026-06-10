package poc.server.thread;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import poc.server.JavaServerMain;
import poc.server.event.IEvent;
import poc.server.event.ShutdownEvent;

public class IncomingConnectionListener implements AutoCloseable {

    private final BlockingQueue<IEvent> mainQueue;
    private final Thread thread;
    private final AtomicBoolean run = new AtomicBoolean(true);

    public IncomingConnectionListener(BlockingQueue<IEvent> queue) {
        this.mainQueue = queue;
        thread = Thread.ofVirtual().name("IncomingConnectionThread").start(this::handleIncomingConnection);
    }

    private void handleIncomingConnection() {
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            channel.configureBlocking(true);
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    JavaServerMain.SERVER_PORT);
            channel.bind(address);
            System.out.println("Listening on " + address);

            while (run.getOpaque()) {
                SocketChannel clientChannel = channel.accept();
                RemoteClient remoteClient = new RemoteClient(clientChannel, mainQueue);
                System.out.println("Client connected: " + clientChannel);
            }
        } catch (IOException e) {
            if (run.getOpaque()) {
                mainQueue.offer(new ShutdownEvent());
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void close() throws InterruptedException {
        run.setOpaque(false);
        thread.interrupt();
        thread.join();
    }
}
