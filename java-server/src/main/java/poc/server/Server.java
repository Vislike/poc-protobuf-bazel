package poc.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import poc.server.event.IEvent;
import poc.server.event.ShutdownEvent;
import poc.server.event.TickEvent;
import poc.server.thread.IncomingConnectionListener;
import poc.server.thread.TickEventGenerator;

public class Server implements AutoCloseable {

    private final BlockingQueue<IEvent> queue;

    public Server() {
        queue = new ArrayBlockingQueue<>(JavaServerMain.MAIN_MESSAGE_QUEUE_SIZE);
    }

    public void start() {
        System.out.println("Starting Java POC Server");
        try (TickEventGenerator _ = new TickEventGenerator(queue, 5000);
                IncomingConnectionListener _ = new IncomingConnectionListener(queue)) {

            mainLoop();

        } catch (InterruptedException e) {
            throw new AssertionError("Unexpected interrupt", e);
        }
    }

    private void mainLoop() throws InterruptedException {
        boolean run = true;
        while (run) {
            IEvent event = queue.take();
            switch (event) {
                case TickEvent e -> System.out.println("Got tick: " + e);
                case ShutdownEvent _ -> run = false;
                default -> System.err.println("Unknown event: " + event);
            }
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
