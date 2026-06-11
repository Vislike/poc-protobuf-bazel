package poc.server.event;

public record UserOutgoingEvent(String userName) implements IEvent {
}
