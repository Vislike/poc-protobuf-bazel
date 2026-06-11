package poc.server.event;

import poc.server.thread.RemoteClient;

public record ClientRemoveEvent(RemoteClient client, String reason) implements IEvent {
}
