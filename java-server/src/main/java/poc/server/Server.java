package poc.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import poc.server.event.ClientAddEvent;
import poc.server.event.ClientRemoveEvent;
import poc.server.event.IEvent;
import poc.server.event.ShutdownEvent;
import poc.server.event.TickEvent;
import poc.server.event.UserDisconnectedEvent;
import poc.server.thread.IncomingConnectionListener;
import poc.server.thread.RemoteClient;
import poc.server.thread.TaskOffload;
import poc.server.thread.TickEventGenerator;

public class Server implements AutoCloseable {

    private final BlockingQueue<IEvent> queue;
    private final List<RemoteClient> clients = new ArrayList<>();

    public Server() {
        queue = new ArrayBlockingQueue<>(JavaServerMain.MAIN_MESSAGE_QUEUE_SIZE);
    }

    public void start() {
        System.out.println("Starting Java POC Server");

        try (TickEventGenerator ticker = new TickEventGenerator(queue, JavaServerMain.SERVER_TICK_FREQUENCY_MS);
                IncomingConnectionListener socketServer = new IncomingConnectionListener(queue);
                TaskOffload task = new TaskOffload(queue)) {

            ticker.start();
            socketServer.start();

            mainLoop(task);

        } catch (InterruptedException e) {
            throw new AssertionError("Unexpected interrupt", e);
        }
    }

    private void mainLoop(TaskOffload task) throws InterruptedException {
        try {
            boolean run = true;
            while (run) {
                IEvent event = queue.take();
                switch (event) {
                    case TickEvent e -> tick(e, task);
                    case ClientAddEvent e -> clients.add(e.client());
                    case ClientRemoveEvent e -> disconnectWithNotify(e, task);
                    case ShutdownEvent _ -> run = false;
                    default -> System.err.println("Unknown event: " + event);
                }
            }
        } finally {
            disconnectAllClients(task);
        }
    }

    private void tick(TickEvent e, TaskOffload task) {
        System.out.println("Got tick: " + e);

        // Remove stale
        for (RemoteClient client : clients) {
            if (client.ageSeconds(JavaServerMain.CLIENT_STALE_TIME_SECONDS)) {
                task.loopbackAsync(new ClientRemoveEvent(client, "Stale connection"));
            }
        }
    }

    private void sendToAllClients(IEvent event, TaskOffload task) {
        for (RemoteClient client : clients) {
            if (!client.clientQueue.offer(event)) {
                task.loopbackAsync(new ClientRemoveEvent(client, "Channel full"));
            }
        }
    }

    private void disconnectWithNotify(ClientRemoveEvent event, TaskOffload task)
            throws InterruptedException {
        RemoteClient client = event.client();

        // Remove exactly one client from list if it still exists
        int assertRemoved = 0;
        for (var it = clients.iterator(); it.hasNext();) {
            if (it.next() == client) {
                it.remove();
                assertRemoved++;
            }
        }
        if (assertRemoved == 1) {
            System.out.println("Disconnecting: " + client + ", reason: " + event.reason());

            // Close socket and notify others
            task.closeAsync(client);
            sendToAllClients(new UserDisconnectedEvent(client.toString()), task);
        } else if (assertRemoved != 0) {
            throw new AssertionError(assertRemoved);
        }
    }

    private void disconnectAllClients(TaskOffload task) {
        while (!clients.isEmpty()) {
            task.closeAsync(clients.removeLast());
        }
    }

    public boolean shutdown() {
        return queue.offer(new ShutdownEvent());
    }

    @Override
    public void close() {
        System.out.println("Shutting down Server");
    }
}
