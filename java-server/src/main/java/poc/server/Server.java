package poc.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import poc.protocol.Chat.Message;
import poc.server.event.IEvent;
import poc.server.event.IEvent.ClientAddEvent;
import poc.server.event.IEvent.ClientRemoveEvent;
import poc.server.event.IEvent.ShutdownEvent;
import poc.server.event.IEvent.TickEvent;
import poc.server.event.IEvent.UserIncomingEvent;
import poc.server.event.IEvent.UserOutgoingEvent;
import poc.server.thread.IncomingConnectionListener;
import poc.server.thread.RemoteClient;
import poc.server.thread.TaskOffload;
import poc.server.thread.TickEventGenerator;
import poc.server.util.Utils;

public class Server implements AutoCloseable {

    private final BlockingQueue<IEvent> queue;
    private final List<RemoteClient> clients = new ArrayList<>();

    private long messagesSent = 0;
    private long lastMessages = 0;
    private long lastTick;

    public Server() {
        queue = new ArrayBlockingQueue<>(JavaServerMain.MAIN_MESSAGE_QUEUE_SIZE);
        this.lastTick = Utils.tickMs();
    }

    public void start() {
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
                    case ShutdownEvent _ -> run = false;
                    case TickEvent e -> tick(e, task);
                    case ClientAddEvent e -> clients.add(e.client());
                    case ClientRemoveEvent e -> disconnectWithNotify(e, task);
                    case UserIncomingEvent e -> userMessage(e, task);
                    default -> System.err.println("Unknown event: " + event);
                }
            }
        } finally {
            disconnectAllClients(task);
        }
    }

    private void userMessage(UserIncomingEvent event, TaskOffload task) {
        Message message = event.message();
        switch (message.getPayloadCase()) {
            case HELLO -> {
                event.client().userName = message.getHello().getUser().getUserName();
                sendToActiveClients(ChatProtocol.systemConnected(event.client().userName), task);
                event.client().active = true;
            }
            case CHAT -> {
                if (event.client().active) {
                    sendToActiveClients(ChatProtocol.chat(message.getChat().getText(), event.client().userName), task);
                } else {
                    task.loopbackAsync(new ClientRemoveEvent(event.client(), "Protocol violation"));
                }
            }
            default -> System.err.println("Unknown message: " + message);
        }
    }

    private void tick(TickEvent e, TaskOffload task) {
        // Some stats
        long messages = messagesSent - lastMessages;
        long elapsedMs = e.tickMs() - lastTick;
        StringBuilder sb = new StringBuilder(64);
        sb.append("Tick: ").append(Utils.largeNumbers(TimeUnit.MILLISECONDS.toSeconds(e.tickMs())));
        if (elapsedMs > 0) {
            sb.append(", M/s: ").append(Utils.largeNumbers(messages * 1000 / elapsedMs));
        }
        sb.append(", Mess: ").append(Utils.largeNumbers(messages));
        sb.append(", Tot: ").append(Utils.largeNumbers(messagesSent));
        System.out.println(sb.toString());
        lastMessages = messagesSent;
        lastTick = e.tickMs();

        // Remove stale & ping old
        for (RemoteClient client : clients) {
            if (client.ageSeconds(JavaServerMain.CLIENT_STALE_TIME_SECONDS, e.tickMs())) {
                task.loopbackAsync(new ClientRemoveEvent(client, "Stale connection"));
            } else if (client.ageSeconds(JavaServerMain.CLIENT_PING_TIME_SECONDS, e.tickMs())) {
                sendToClient(client, ChatProtocol.ping(), task);
            }
        }
    }

    private void sendToClient(RemoteClient client, Message message, TaskOffload task) {
        UserOutgoingEvent event = IEvent.createUserOutgoingEvent(message);

        offerClientHandleFull(client, event, task);
    }

    private void sendToActiveClients(Message message, TaskOffload task) {
        UserOutgoingEvent event = IEvent.createUserOutgoingEvent(message);

        for (RemoteClient client : clients) {
            if (client.active) {
                offerClientHandleFull(client, event, task);
            }
        }
    }

    private void offerClientHandleFull(RemoteClient client, IEvent event, TaskOffload task) {
        if (client.clientQueue.offer(event)) {
            messagesSent++;
        } else {
            task.loopbackAsync(new ClientRemoveEvent(client, "Channel full"));
        }
    }

    private void disconnectWithNotify(ClientRemoveEvent event, TaskOffload task) {
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
