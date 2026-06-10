package poc.server.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import poc.server.event.IEvent;
import poc.server.event.TickEvent;
import poc.server.util.Utils;

public class TickEventGenerator implements AutoCloseable {

    private final BlockingQueue<IEvent> queue;
    private final long ms;
    private final ScheduledExecutorService executor;

    public TickEventGenerator(BlockingQueue<IEvent> queue, long ms) {
        this.queue = queue;
        this.ms = ms;
        executor = Executors
                .newSingleThreadScheduledExecutor(Thread.ofVirtual().name("TickEventGeneratorThread").factory());
    }

    public void start() {
        executor.scheduleAtFixedRate(this::eventGenerator, ms, ms, TimeUnit.MILLISECONDS);
    }

    private void eventGenerator() {
        queue.offer(new TickEvent(Utils.tickMs()));
    }

    @Override
    public void close() {
        executor.close();
    }
}
