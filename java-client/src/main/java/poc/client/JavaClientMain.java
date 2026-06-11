package poc.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import poc.protocol.Chat.ChatMessage;
import poc.protocol.Chat.Message;
import poc.protocol.Chat.SystemMessage;

public class JavaClientMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Java Client");
        System.out.println("Connecting to localhost:5000");
        try (SocketChannel channel = SocketChannel
                .open(new InetSocketAddress(InetAddress.getLoopbackAddress(), 5000))) {
            System.out.println("Connected, Sending hello world with protobuf");

            Message message = Message.newBuilder()
                    .setChat(ChatMessage.newBuilder().setText("Hello world using protobuf")
                            .setChannelId("HiddenChannel"))
                    .build();
            send(message, channel);

            Message message2 = Message.newBuilder()
                    .setChat(ChatMessage.newBuilder().setText("msg2")).setSenderId("TestSender")
                    .build();
            send(message2, channel);

            Message message3 = Message.newBuilder()
                    .setSystem(SystemMessage.newBuilder().setAlertLevel("high")).build();
            send(message3, channel);
        }
        System.out.println("Client finished");
    }

    private static void send(Message message, SocketChannel channel) throws IOException {
        ByteBuffer lengthBB = ByteBuffer.allocate(2);
        ByteBuffer msgBB = ByteBuffer.wrap(message.toByteArray());
        lengthBB.putShort((short) msgBB.capacity());
        channel.write(lengthBB.rewind());
        channel.write(msgBB);
    }
}
