package poc.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import poc.protocol.Chat.ChatMessage;
import poc.protocol.Chat.Message;
import poc.protocol.Chat.SystemMessage;

public class JavaClientMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        ByteBuffer le = ByteBuffer.allocate(4);
        le.order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer be = ByteBuffer.allocate(4);
        le.putInt(25);
        be.putInt(25);

        System.out.println(Arrays.toString(le.array()) + " " + le.rewind().getInt());
        System.out.println(Arrays.toString(be.array()) + " " + be.rewind().getInt());

        System.out.println("Java Client");
        System.out.println("Connecting to localhost:5000");
        ByteBuffer bb = ByteBuffer.allocate(1024);
        try (SocketChannel channel = SocketChannel
                .open(new InetSocketAddress(InetAddress.getLoopbackAddress(), 5000))) {
            System.out.println("Connected");
            System.out.println("Is Blocking: " + channel.isBlocking());
            System.out.println("Sending hello world with protobuf");

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
        System.out.println("Sending length: " + msgBB.capacity());
        lengthBB.putShort((short) msgBB.capacity());
        channel.write(lengthBB.rewind());
        channel.write(msgBB);
    }
}
