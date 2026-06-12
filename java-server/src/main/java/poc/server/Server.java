package poc.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import poc.protocol.Chat.Message;
import poc.server.event.ClientAddEvent;
import poc.server.event.ClientRemoveEvent;
import poc.server.event.IEvent;
import poc.server.event.ShutdownEvent;
import poc.server.event.TickEvent;
import poc.server.event.UserIncomingEvent;
import poc.server.event.UserOutgoingEvent;
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
                    case UserIncomingEvent e -> chatEvent(e, task);
                    default -> System.err.println("Unknown event: " + event);
                }
            }
        } finally {
            disconnectAllClients(task);
        }
    }

    private void chatEvent(UserIncomingEvent event, TaskOffload task) {
        Message message = event.message();
        switch (message.getPayloadCase()) {
            case HELLO -> {
                event.client().active = true;
                event.client().userName = message.getHello().getUser().getUserName();
                sendToActiveClients(ChatProtocol.systemConnected(event.client().userName), task);
            }
            case CHAT -> {
                sendToActiveClients(ChatProtocol.chat(message.getChat().getText(), event.client().userName), task);
            }
            default -> System.out.println("Unknown message: " + message);
        }
    }

    private void tick(TickEvent e, TaskOffload task) {
        System.out.println("Got tick: " + e);

        // Remove stale & ping old
        for (RemoteClient client : clients) {
            if (client.ageSeconds(JavaServerMain.CLIENT_STALE_TIME_SECONDS)) {
                task.loopbackAsync(new ClientRemoveEvent(client, "Stale connection"));
            } else if (client.ageSeconds(JavaServerMain.CLIENT_PING_TIME_SECONDS)) {
                sendToClient(client, ChatProtocol.ping(), task);
            }
        }
    }

    private void sendToClient(RemoteClient client, Message message, TaskOffload task) {
        ByteBuffer bb = ByteBuffer.wrap(message.toByteArray());
        UserOutgoingEvent event = new UserOutgoingEvent(bb);

        if (!client.clientQueue.offer(event)) {
            task.loopbackAsync(new ClientRemoveEvent(client, "Channel full"));
        }
    }

    private void sendToActiveClients(Message message, TaskOffload task) {
        ByteBuffer bb = ByteBuffer.wrap(message.toByteArray());
        UserOutgoingEvent event = new UserOutgoingEvent(bb);

        for (RemoteClient client : clients) {
            if (client.active) {
                if (!client.clientQueue.offer(event)) {
                    task.loopbackAsync(new ClientRemoveEvent(client, "Channel full"));
                }
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
            if (client.active) {
                sendToActiveClients(ChatProtocol.systemDisconnected(client.userName), task);
            }
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
