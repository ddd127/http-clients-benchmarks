package com.example.benchmark.utils.queue;

import java.util.concurrent.BlockingQueue;

public class QueueTest {
    public static void main(String[] args) {
        final BlockingQueue<Integer> queue = new NonBlockingQueue<>();
        for (int i = 0; i < 100; ++i) {
            queue.add(i);
            if (queue.poll() != i) {
                throw new IllegalStateException();
            }
        }
    }
}
