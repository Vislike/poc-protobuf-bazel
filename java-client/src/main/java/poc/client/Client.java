package poc.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import poc.JavaClientMain;
import poc.client.event.IEvent;
import poc.client.event.IEvent.PingEvent;
import poc.client.event.IEvent.ShutdownEvent;
import poc.client.event.IEvent.StdinEvent;
import poc.client.thread.ReceiveThread;
import poc.client.thread.StdinThread;
import poc.client.util.Terminal;
import poc.protocol.Chat.Message;

public class Client implements AutoCloseable {

    protected final BlockingQueue<IEvent> eventQueue;

    protected SocketChannel channel;
    private ByteBuffer sendLengthBB = ByteBuffer.allocate(2);

    public Client() {
        eventQueue = new LinkedBlockingQueue<>();
    }

    public void start() throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            // Ask for username
            System.out.print("Enter username: ");
            String userName = scanner.nextLine();
            if (userName.length() < JavaClientMain.NAME_MIN_LENGTH
                    || userName.length() > JavaClientMain.NAME_MAX_LENGTH) {
                Terminal.redMessage("Please use a name within " + JavaClientMain.NAME_MIN_LENGTH + "-"
                        + JavaClientMain.NAME_MAX_LENGTH + " characters:",
                        String.valueOf(userName.length()));
                return;
            }

            // Connect
            connect(userName);
            Terminal.systemMessage("Connected, type /quit or /exit to stop.");

            // Start helper threads
            try (ReceiveThread receiveThread = new ReceiveThread(eventQueue, channel, userName);
                    StdinThread stdinThread = new StdinThread(eventQueue)) {

                receiveThread.start();
                stdinThread.start();

                mainLoop();
            }
        } catch (InterruptedException e) {
            throw new AssertionError("Unexpected interrupt", e);
        }
    }

    private void mainLoop() throws IOException, InterruptedException {
        Set<String> stop = new HashSet<>(List.of("/q", "/quit", "/e", "/exit"));
        boolean run = true;

        while (run) {
            IEvent event = eventQueue.take();
            switch (event) {
                case ShutdownEvent _ -> run = false;
                case PingEvent _ -> send(ChatProtocol.pong());
                case StdinEvent e -> {
                    if (stop.contains(e.text())) {
                        run = false;
                    } else if (e.text().length() > 0) {
                        send(ChatProtocol.chat(e.text()));
                    }
                }
            }
        }
    }

    protected void connect(String userName) throws IOException {
        Terminal.systemMessage("Connecting to localhost:" + JavaClientMain.PORT);
        channel = SocketChannel.open(new InetSocketAddress(InetAddress.getLoopbackAddress(), JavaClientMain.PORT));

        send(ChatProtocol.hello(userName));
    }

    protected void send(Message message) throws IOException {
        ByteBuffer msgBB = ByteBuffer.wrap(message.toByteArray());
        sendLengthBB.clear().putShort((short) msgBB.capacity());
        channel.write(sendLengthBB.rewind());
        channel.write(msgBB);
    }

    @Override
    public void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
