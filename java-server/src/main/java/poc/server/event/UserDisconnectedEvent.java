package poc.server.event;

public record UserDisconnectedEvent(String userName) implements IEvent {
}
