package poc.server;

import java.time.Duration;

public class JavaServerMain {

    public static final int SERVER_PORT = 5000;
    public static final int MAIN_MESSAGE_QUEUE_SIZE = 128;
    public static final int CLIENT_MESSAGE_QUEUE_SIZE = 16;
    public static final int MAX_SHUTDOWN_SECONDS = 5;

    public static void main(String[] args) throws Exception {
        try (Server server = new Server()) {
            addShutdownHook(server);

            server.start();
        }

        System.out.println("Bye");
    }

    private static void addShutdownHook(Server server) {
        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.shutdown();
                if (!mainThread.join(Duration.ofSeconds(5))) {
                    System.err.println("Server failed to shutdown within " + MAX_SHUTDOWN_SECONDS
                            + "s time limit... Hard exiting");
                }
            } catch (InterruptedException e) {
                throw new AssertionError("Unexpected interrupt", e);
            }
        }, "ShutdownThread"));
    }
}
