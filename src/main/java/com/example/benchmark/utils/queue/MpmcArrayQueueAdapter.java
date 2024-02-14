package com.example.benchmark.utils.queue;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jctools.queues.MpmcArrayQueue;

public class MpmcArrayQueueAdapter<T> extends MpmcArrayQueue<T> implements BlockingQueue<T> {

    public MpmcArrayQueueAdapter(int capacity) {
        super(capacity);
    }

    @Override
    public void put(T t) throws InterruptedException {
        while (!this.offer(t)) {
            // continue
        }
    }

    @Override
    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        return this.offer(t);
    }

    @Override
    public T take() throws InterruptedException {
        T item;
        while ((item = this.poll()) == null) {
            // continue
        }
        return item;
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return this.poll();
    }

    @Override
    public int remainingCapacity() {
        return this.capacity() - this.size();
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        T item;
        int count = 0;
        while ((item = this.poll()) != null) {
            c.add(item);
            ++count;
        }
        return count;
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        throw new UnsupportedOperationException();
    }
}
