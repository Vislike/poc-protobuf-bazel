package poc;

import java.io.IOException;
import java.util.Optional;

import poc.client.AutoClient;
import poc.client.Client;
import poc.client.util.Terminal;

public class JavaClientMain {

    public static final int NAME_MIN_LENGTH = 2;
    public static final int NAME_MAX_LENGTH = 20;
    public static final long TERMINAL_SLEEP_TIME_MS = 50;

    public static final int AUTO_STARTUP_DELAY_SEC = 60;
    public static final int AUTO_DISCONNECT_CHANCE_PERCENT = 0;
    public static final int AUTO_CHAT_DELAY_FIRST_SEC = 30;
    public static final int AUTO_CHAT_DELAY_MIN_MS = 1;
    public static final int AUTO_CHAT_DELAY_MAX_MS = 200;

    public static void main(String[] args) throws Exception {
        System.out.println("Java POC Client");

        Optional<Client> client = createClient(args);

        client.ifPresent(c -> {
            try {
                c.start();
            } catch (IOException e) {
                Terminal.redMessage("Network problem", e.getMessage());
            } finally {
                c.close();
            }
        });

        System.out.println("Java Client Ended");
    }

    private static Optional<Client> createClient(String[] args) {
        boolean auto = false;
        boolean max = false;

        for (String arg : args) {
            switch (arg.toLowerCase()) {
                case "-a", "--auto" -> auto = true;
                case "-m", "--max" -> max = true;
                default -> {
                    Terminal.redMessage("Unknown argument", arg);
                    Terminal.systemMessage("""
                            Help:
                            <no arg>       Interactive mode
                            --auto (-a)    Auto chat mode.
                            --max (-m)     Test send max length chat message. (Require -a)""");
                    return Optional.empty();
                }
            }
        }
        return Optional.of(auto ? new AutoClient(max) : new Client());
    }
}
