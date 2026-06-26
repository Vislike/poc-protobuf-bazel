package poc.client.thread;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import poc.client.event.IEvent;
import poc.client.event.IEvent.PingEvent;
import poc.client.event.IEvent.ShutdownEvent;
import poc.client.util.Terminal;
import poc.protocol.Chat.ChatMessage;
import poc.protocol.Chat.Message;
import poc.protocol.Chat.SystemMessage;

public class ReceiveThread implements AutoCloseable {

    private final Queue<IEvent> mainQueue;
    private final SocketChannel channel;
    private final String userName;
    private final AtomicBoolean run = new AtomicBoolean(true);

    private Thread thread;

    public ReceiveThread(Queue<IEvent> queue, SocketChannel channel, String userName) {
        this.mainQueue = queue;
        this.channel = channel;
        this.userName = userName;
    }

    public void start() {
        thread = Thread.ofPlatform().name("ReceiveThread").start(this::receiveThread);
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
                mainQueue.add(new ShutdownEvent());
                switch (e) {
                    case EOFException _ -> Terminal.systemMessage("Server closed connection");
                    default -> throw new AssertionError(e);
                }
            }
        }
    }

    private void chatMessage(Message message) {
        switch (message.getPayloadCase()) {
            case PING -> {
                mainQueue.add(new PingEvent());
            }
            case SYSTEM -> {
                SystemMessage system = message.getSystem();
                switch (system.getType()) {
                    case USER_CONNECTED -> Terminal.userConnected(system.getUser().getUserName());
                    case USER_DISCONNECTED -> Terminal.userDisconnected(system.getUser().getUserName());
                    default -> Terminal.redMessage(system.getUser().getUserName(), "unknown status");
                }
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
            default -> Terminal.redMessage("Unknown message", message.toString());
        }
    }

    private void readUntilEOF(ByteBuffer bb) throws IOException {
        while (bb.hasRemaining()) {
            if (channel.read(bb) == -1) {
                throw new EOFException();
            }
        }
    }

    @Override
    public void close() throws InterruptedException {
        run.setOpaque(false);
        thread.interrupt();
        thread.join();
    }
}
