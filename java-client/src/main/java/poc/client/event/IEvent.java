package poc.client.event;

public sealed interface IEvent {

    record ShutdownEvent() implements IEvent {
    }

    record PingEvent() implements IEvent {
    }

    record StdinEvent(String text) implements IEvent {
    }
}
