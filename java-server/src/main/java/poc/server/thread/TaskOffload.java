package poc.server.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import poc.server.event.IEvent;

public class TaskOffload implements AutoCloseable {

    private final ExecutorService executor;
    private final BlockingQueue<IEvent> queue;

    public TaskOffload(BlockingQueue<IEvent> queue) {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        this.queue = queue;
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

    public void loopbackAsync(IEvent event) {
        executor.execute(() -> {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                throw new AssertionError("Unexpected interrupt", e);
            }
        });
    }

    @Override
    public void close() {
        executor.close();
    }
}
