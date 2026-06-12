package poc.client;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import poc.client.IEvent.PingEvent;
import poc.protocol.Chat.ChatMessage;
import poc.protocol.Chat.Message;
import poc.protocol.Chat.SystemMessage;

public class Client implements AutoCloseable {

    private final Thread mainThread;
    private final AtomicBoolean run = new AtomicBoolean(true);
    private final Queue<IEvent> eventQueue;

    private Thread receiveThread;
    private SocketChannel channel;
    private final AtomicBoolean receiveThreadStarted = new AtomicBoolean(false);
    private String userName;

    private ByteBuffer sendLengthBB = ByteBuffer.allocate(2);

    public Client() {
        mainThread = Thread.currentThread();
        eventQueue = new ConcurrentLinkedQueue<>();
    }

    public void start() throws IOException {
        System.out.println("Java Client");
        try (BufferedReader stdinReader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Enter username: ");
            userName = stdinReader.readLine();
            if (userName.length() < JavaClientMain.NAME_MIN_LENGTH) {
                System.err.println("Please use a name with a least " + JavaClientMain.NAME_MIN_LENGTH + " characters.");
                return;
            }

            connect();
            Terminal.systemMessage("Connected, type /quit or /exit to stop.");
            startReceiveThread();

            chatLoop(stdinReader);

        } catch (InterruptedException e) {
            if (run.getOpaque()) {
                throw new AssertionError("Unexpected interrupt", e);
            }
        }
    }

    private void chatLoop(BufferedReader stdinReader) throws IOException, InterruptedException {
        Set<String> stop = new HashSet<>(List.of("/q", "/quit", "/e", "/exit"));
        IEvent event = null;

        while (run.getOpaque()) {
            if (stdinReader.ready()) {
                String line = stdinReader.readLine();

                if (stop.contains(line)) {
                    run.setOpaque(false);
                } else if (line.length() > 0) {
                    send(ChatProtocol.chat(line));
                }
            } else if ((event = eventQueue.poll()) != null) {
                switch (event) {
                    case PingEvent _ -> send(ChatProtocol.pong());
                }
            } else {
                // Idle the cpu
                Thread.sleep(JavaClientMain.TERMINAL_SLEEP_TIME_MS);
            }
        }

    }

    public void auto() throws IOException, InterruptedException {
        System.out.println("Java Auto Client");
        Random random = new Random();
        userName = "Anon " + random.nextInt(1, 100);
        connect();
        Terminal.systemMessage("Connected, Auto chat mode");
        startReceiveThread();

        send(ChatProtocol.chat("Hello world using protobuf"));
        send(ChatProtocol.chat("msg2"));
        Thread.sleep(2000);
    }

    private void connect() throws IOException {
        System.out.println("Connecting to localhost:5000");
        channel = SocketChannel.open(new InetSocketAddress(InetAddress.getLoopbackAddress(), 5000));

        send(ChatProtocol.hello(userName));
    }

    private void send(Message message) throws IOException {
        ByteBuffer msgBB = ByteBuffer.wrap(message.toByteArray());
        sendLengthBB.clear().putShort((short) msgBB.capacity());
        channel.write(sendLengthBB.rewind());
        channel.write(msgBB);
    }

    private void readUntilEOF(ByteBuffer bb) throws IOException {
        while (bb.hasRemaining()) {
            if (channel.read(bb) == -1) {
                throw new EOFException();
            }
        }
    }

    private void startReceiveThread() {
        if (!receiveThreadStarted.compareAndSet(false, true)) {
            throw new IllegalStateException("Already started");
        }

        receiveThread = Thread.ofPlatform().name("ReceiveThread").start(this::receiveThread);
    }

    private void receiveThread() {
        try {
            ByteBuffer lengthBB = ByteBuffer.allocate(2);
            while (run.getOpaque()) {
                // Receive message length
                readUntilEOF(lengthBB.clear());

                // Receive message
                ByteBuffer msgBB = ByteBuffer.allocate(Short.toUnsignedInt(lengthBB.rewind().getShort()));
                readUntilEOF(msgBB);

                // Decode
                Message message = Message.parseFrom(msgBB.rewind());
                chatMessage(message);
            }
        } catch (IOException e) {
            if (run.getOpaque()) {
                switch (e) {
                    case EOFException _ -> {
                        Terminal.systemMessage("Server closed connection");
                        run.setOpaque(false);
                        mainThread.interrupt();
                    }
                    default -> throw new AssertionError(e);
                }
            }
        }
    }

    private void chatMessage(Message message) {
        switch (message.getPayloadCase()) {
            case PING -> {
                eventQueue.add(new PingEvent());
            }
            case SYSTEM -> {
                SystemMessage system = message.getSystem();
                Terminal.userEvent(system.getUser().getUserName(), switch (system.getType()) {
                    case USER_CONNECTED -> "connected";
                    case USER_DISCONNECTED -> "disconnected";
                    default -> "unknown status";
                });
            }
            case CHAT -> {
                ChatMessage chat = message.getChat();
                String name = chat.getUser().getUserName();
                if (name.equals(userName)) {
                    Terminal.chatSelf(name, chat.getText());
                } else {
                    Terminal.chatOther(name, chat.getText());
                }
            }
            default -> Terminal.unknownMessage("Unknown message", message.toString());
        }
    }

    @Override
    public void close() throws InterruptedException, IOException {
        run.setOpaque(false);
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread.join();
        }
        if (channel != null) {
            channel.close();
        }
    }
}
