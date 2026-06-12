package poc.server;

import java.util.function.Consumer;

import poc.protocol.Chat.Message;
import poc.protocol.Chat.SystemMessage.Type;
import poc.protocol.Chat.User;

public class ChatProtocol {

    private ChatProtocol() {
    }

    private static Message message(Consumer<Message.Builder> builderConsumer) {
        Message.Builder builder = Message.newBuilder();
        builderConsumer.accept(builder);
        return builder.build();
    }

    private static User.Builder user(String name) {
        return User.newBuilder().setUserName(name);
    }

    public static Message ping() {
        return message(b -> b.getPingBuilder());
    }

    public static Message systemConnected(String userName) {
        return message(b -> b.getSystemBuilder().setType(Type.USER_CONNECTED).setUser(user(userName)));
    }

    public static Message systemDisconnected(String userName) {
        return message(b -> b.getSystemBuilder().setType(Type.USER_DISCONNECTED).setUser(user(userName)));
    }

    public static Message chat(String text, String userName) {
        return message(b -> b.getChatBuilder().setText(text).setUser(user(userName)));
    }
}
