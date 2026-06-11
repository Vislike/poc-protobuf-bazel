package poc.server.thread;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import poc.server.JavaServerMain;
import poc.server.event.ClientAddEvent;
import poc.server.event.IEvent;
import poc.server.event.ShutdownEvent;

public class IncomingConnectionListener implements AutoCloseable {

    private final BlockingQueue<IEvent> queue;
    private final AtomicBoolean run = new AtomicBoolean(true);

    private Thread thread;

    public IncomingConnectionListener(BlockingQueue<IEvent> queue) {
        this.queue = queue;
    }

    public void start() {
        thread = Thread.ofVirtual().name("IncomingConnectionThread").start(this::handleIncomingConnection);
    }

    private void handleIncomingConnection() {
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            channel.configureBlocking(true);
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(),
                    JavaServerMain.SERVER_PORT);
            channel.bind(address);
            System.out.println("Listening on " + address);

            // Accept loop
            while (run.getOpaque()) {
                RemoteClient remoteClient = new RemoteClient(channel.accept(), queue);
                System.out.println("Client connected: " + remoteClient);
                remoteClient.start();
                queue.put(new ClientAddEvent(remoteClient));
            }
        } catch (Exception e) {
            if (run.getOpaque()) {
                queue.offer(new ShutdownEvent());
                throw new AssertionError(e);
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
