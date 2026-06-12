package poc.client;

import java.util.function.Consumer;

import poc.protocol.Chat.Message;
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

    public static Message pong() {
        return message(b -> b.getPongBuilder());
    }

    public static Message chat(String text) {
        return message(b -> b.getChatBuilder().setText(text));
    }

    public static Message hello(String name) {
        return message(b -> b.getHelloBuilder().setUser(user(name)));
    }
}
