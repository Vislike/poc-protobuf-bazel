package poc.server.event;

import poc.protocol.Chat.Message;
import poc.server.thread.RemoteClient;

public record UserIncomingEvent(RemoteClient client, Message message) implements IEvent {

}
