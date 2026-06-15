package poc.server;

import java.time.Duration;

public class JavaServerMain {

    public static final int SERVER_PORT = 5000;
    public static final int SERVER_TICK_FREQUENCY_MS = 5000;
    public static final int MAIN_MESSAGE_QUEUE_SIZE = 32;
    public static final int SHUTDOWN_SECONDS_MAX = 5;

    public static final int CLIENT_MESSAGE_QUEUE_SIZE = 1024;
    public static final int CLIENT_PING_TIME_SECONDS = 10;
    public static final int CLIENT_STALE_TIME_SECONDS = 20;

    public static void main(String[] args) throws Exception {
        System.out.println("Java POC Server");

        try (Server server = new Server()) {
            addShutdownHook(server);

            server.start();
        }

        System.out.println("Java Server Ended");
    }

    private static void addShutdownHook(Server server) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (server.shutdown()) {
                    if (!mainThread.join(Duration.ofSeconds(SHUTDOWN_SECONDS_MAX))) {
                        System.err.println("Server failed to shutdown within " + SHUTDOWN_SECONDS_MAX
                                + "s time limit... Hard exiting");
                    }
                } else {
                    System.err.println("Server did not accept shutdown command... Hard exiting");
                }
            } catch (InterruptedException e) {
                throw new AssertionError("Unexpected interrupt", e);
            }
        }, "ShutdownThread"));
    }
}
