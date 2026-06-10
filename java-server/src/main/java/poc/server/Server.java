package poc.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import poc.server.event.ClientAddEvent;
import poc.server.event.IEvent;
import poc.server.event.ShutdownEvent;
import poc.server.event.TickEvent;
import poc.server.thread.ConnectionCloser;
import poc.server.thread.IncomingConnectionListener;
import poc.server.thread.RemoteClient;
import poc.server.thread.TickEventGenerator;

public class Server implements AutoCloseable {

    private final BlockingQueue<IEvent> queue;
    private final List<RemoteClient> clients = new ArrayList<>();

    public Server() {
        queue = new ArrayBlockingQueue<>(JavaServerMain.MAIN_MESSAGE_QUEUE_SIZE);
    }

    public void start() {
        System.out.println("Starting Java POC Server");

        try (TickEventGenerator ticker = new TickEventGenerator(queue, 5000);
                IncomingConnectionListener socketServer = new IncomingConnectionListener(queue);
                ConnectionCloser closeHandler = new ConnectionCloser()) {

            ticker.start();
            socketServer.start();

            mainLoop(closeHandler);

        } catch (InterruptedException e) {
            throw new AssertionError("Unexpected interrupt", e);
        }
    }

    private void mainLoop(ConnectionCloser closeHandler) throws InterruptedException {
        try {
            boolean run = true;
            while (run) {
                IEvent event = queue.take();
                switch (event) {
                    case TickEvent e -> System.out.println("Got tick: " + e);
                    case ClientAddEvent e -> clients.add(e.client());
                    case ShutdownEvent _ -> run = false;
                    default -> System.err.println("Unknown event: " + event);
                }
            }
        } finally {
            disconnectAllClients(closeHandler);
        }
    }

    private void disconnectAllClients(ConnectionCloser closeHandler) {
        while (!clients.isEmpty()) {
            closeHandler.closeAsync(clients.removeLast());
        }
    }

    public void shutdown() throws InterruptedException {
        queue.put(new ShutdownEvent());
    }

    @Override
    public void close() {
        System.out.println("Shutting down Server");
    }
}
