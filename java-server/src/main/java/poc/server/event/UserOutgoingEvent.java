package poc.server.event;

import java.nio.ByteBuffer;

public record UserOutgoingEvent(ByteBuffer sharedBB) implements IEvent {
}
