package poc.server.event;

public record TickEvent(long tickMs) implements IEvent {
}
