package poc.server.event;

import java.nio.ByteBuffer;

import poc.protocol.Chat.Message;
import poc.server.thread.RemoteClient;

public sealed interface IEvent {

    record ShutdownEvent() implements IEvent {
    }

    record TickEvent(long tickMs) implements IEvent {
    }

    record ClientAddEvent(RemoteClient client) implements IEvent {
    }

    record ClientRemoveEvent(RemoteClient client, String reason) implements IEvent {
    }

    record UserIncomingEvent(RemoteClient client, Message message) implements IEvent {
    }

    record UserOutgoingEvent(ByteBuffer sharedBB) implements IEvent {
    }

    static UserOutgoingEvent createUserOutgoingEvent(Message message) {
        return new UserOutgoingEvent(ByteBuffer.wrap(message.toByteArray()));
    }
}
