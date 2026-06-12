package poc.client;

public sealed interface IEvent {

    record PingEvent() implements IEvent {
    }
}
