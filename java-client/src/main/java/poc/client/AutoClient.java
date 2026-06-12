package poc.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Random;

import poc.client.event.IEvent;
import poc.client.event.IEvent.ShutdownEvent;
import poc.client.thread.ReceiveThread;
import poc.client.util.Terminal;

public class AutoClient extends Client {

    @Override
    public void start() throws IOException {
        Random random = new Random();

        // Start delay
        long startDelay = random.nextLong(10000);
        Terminal.chatOther("Random start delay", String.valueOf(startDelay));
        try {
            Thread.sleep(startDelay);
        } catch (InterruptedException e) {
            // Defer interrupt to next
            Thread.currentThread().interrupt();
        }

        // Username
        String userName = "Anon " + random.nextInt(1, 1000);

        // Connect
        connect(userName);
        Terminal.systemMessage("Connected, auto chat mode");

        // Start receive thread
        try (ReceiveThread receiveThread = new ReceiveThread(eventQueue, channel, userName)) {
            receiveThread.start();

            // Handle server close
            Thread mainThread = Thread.currentThread();
            Thread.ofPlatform().daemon(true).start(() -> {
                try {
                    boolean run = true;
                    while (run) {
                        IEvent event = eventQueue.take();
                        switch (event) {
                            case ShutdownEvent _ -> run = false;
                            default -> System.out.println("Ignore: " + event);
                        }
                    }
                } catch (InterruptedException e) {
                }
                mainThread.interrupt();
            });

            // Simulate some chat
            while (random.nextInt(50) > 0) {
                // Delay before
                Thread.sleep(Duration.ofSeconds(random.nextInt(10)));

                // Random chat
                send(ChatProtocol.chat(switch (random.nextInt(10)) {
                    case 0 -> "Hello world using protobuf!";
                    case 1 -> "My name is " + userName + " i am " + (random.nextInt(20) + 5) + " years old.";
                    case 2 -> "Have a wonderful day!";
                    case 3 -> "How are you?";
                    case 4 -> "Thanks \u2764";
                    case 5 -> "Good for you :)";
                    case 6 -> "I am happy.";
                    case 7 -> "What is your name?";
                    case 8 -> "It is sunny outside \u2600";
                    default -> "Here is a random number: " + (random.nextInt(1000) + 1);
                }));

                // Delay after
                Thread.sleep(Duration.ofSeconds(random.nextInt(20)));
            }
            Thread.sleep(1000);
        } catch (InterruptedException _) {
            // Ignore in auto mode
        }
    }
}
