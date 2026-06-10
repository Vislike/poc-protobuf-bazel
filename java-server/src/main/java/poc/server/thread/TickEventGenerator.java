package poc.server.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import poc.server.event.IEvent;
import poc.server.event.TickEvent;
import poc.server.util.Utils;

public class TickEventGenerator implements AutoCloseable {

    private final ScheduledExecutorService executor;

    public TickEventGenerator(BlockingQueue<IEvent> queue, long ms) {
        executor = Executors
                .newSingleThreadScheduledExecutor(Thread.ofVirtual().name("TickEventGeneratorThread").factory());
        executor.scheduleAtFixedRate(eventGenerator(queue), ms, ms, TimeUnit.MILLISECONDS);
    }

    private Runnable eventGenerator(BlockingQueue<IEvent> queue) {
        return () -> {
            queue.offer(new TickEvent(Utils.tickMs()));
        };
    }

    @Override
    public void close() {
        executor.close();
    }
}
