package poc.client;

public class JavaClientMain {

    public static final int NAME_MIN_LENGTH = 2;
    public static final long TERMINAL_SLEEP_TIME_MS = 50;

    public static void main(String[] args) throws Exception {
        try (Client client = new Client()) {
            if (args.length > 0) {
                if (args[0].equals("-a") || args[0].equals("--auto")) {
                    client.auto();
                } else {
                    System.err.println("Unknown argument, run without for interactive, or --auto (-a) for auto mode.");
                }
            } else {
                client.start();
            }
        }
        System.out.println("Client finished");
    }
}
