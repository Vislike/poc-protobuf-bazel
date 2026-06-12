package poc.client.thread;

import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import poc.client.event.IEvent;
import poc.client.event.IEvent.StdinEvent;

public class StdinThread implements AutoCloseable {

    private final Queue<IEvent> mainQueue;
    private final AtomicBoolean run = new AtomicBoolean(true);

    private Thread thread;

    public StdinThread(Queue<IEvent> queue) {
        this.mainQueue = queue;
    }

    public void start() {
        thread = Thread.ofPlatform().daemon(true).name("StdinThread").start(this::stdinThread);
    }

    private void stdinThread() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (run.getOpaque()) {
                String line = scanner.nextLine();
                mainQueue.add(new StdinEvent(line));
            }
        }
    }

    @Override
    public void close() {
        run.setOpaque(false);
        thread.interrupt();
    }
}
