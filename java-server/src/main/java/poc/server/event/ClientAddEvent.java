package poc.server.event;

import poc.server.thread.RemoteClient;

public record ClientAddEvent(RemoteClient client) implements IEvent {
}
