package poc.server.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionCloser implements AutoCloseable {

    private final ExecutorService executor;

    public ConnectionCloser() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void closeAsync(RemoteClient client) {
        executor.execute(() -> {
            try {
                client.close();
            } catch (Exception e) {
                throw new AssertionError("Unexpected problem disconnecting client: " + client, e);
            }
        });
    }

    @Override
    public void close() {
        executor.close();
    }
}
