package com.example.benchmark.utils.queue;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.openjdk.jmh.infra.Blackhole;

public class CheatingQueue<ITEM> extends AbstractQueue<ITEM> implements BlockingQueue<ITEM> {

    public static final int DEFAULT_SEGMENT_SIZE = 8;
    public static final int DEFAULT_CPU_CONSUMING = 8;
    private final AtomicReference<Segment<ITEM>> head;
    private final AtomicReference<Segment<ITEM>> tail;
    private final AtomicInteger size;
    private final int segmentSize;
    private final int cpuConsuming;

    public CheatingQueue(final int segmentSize, final int cpuConsuming) {
        this.segmentSize = segmentSize;
        this.cpuConsuming = cpuConsuming;
        size = new AtomicInteger(0);
        final Segment<ITEM> initNode = new Segment<>(this.segmentSize);
        head = new AtomicReference<>(initNode);
        tail = new AtomicReference<>(initNode);
    }

    public CheatingQueue(final int segmentSize) {
        this(segmentSize, DEFAULT_CPU_CONSUMING);
    }

    public CheatingQueue() {
        this(DEFAULT_SEGMENT_SIZE, DEFAULT_CPU_CONSUMING);
    }

    @Override
    public void put(ITEM t) throws InterruptedException {
        while (true) {
            if (tryOffer(t)) {
                return;
            }
        }
    }

    @Override
    public boolean offer(ITEM t) {
        while (true) {
            if (tryOffer(t)) {
                return true;
            }
        }
    }

    @Override
    public boolean offer(ITEM t, long timeout, TimeUnit unit) throws InterruptedException {
        final long endNanos = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < endNanos) {
            if (tryOffer(t)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ITEM take() throws InterruptedException {
        while (true) {
            final var pollResult = tryPoll();
            if (pollResult == null) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                continue;
            }
            return pollResult.orElse(null);
        }
    }

    @Override
    public ITEM poll() {
        while (!isEmpty()) {
            final var pollResult = tryPoll();
            if (pollResult != null) {
                return pollResult.orElse(null);
            }
        }
        return null;
    }

    @Override
    public ITEM poll(long timeout, TimeUnit unit) throws InterruptedException {
        final long endNanos = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < endNanos) {
            final var pollResult = tryPoll();
            if (pollResult == null) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                continue;
            }
            return pollResult.orElse(null);
        }
        return null;
    }

    @Override
    public ITEM peek() {
        while (!isEmpty()) {
            final var peekResult = tryPeek();
            if (peekResult != null) {
                return peekResult.orElse(null);
            }
        }
        return null;
    }

    @Override
    public Iterator<ITEM> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainTo(Collection<? super ITEM> c) {
        if (c == this) {
            throw new IllegalArgumentException();
        }
        Segment<ITEM> oldHead;
        final var newHead = new Segment<ITEM>(segmentSize);
        while (true) {
            oldHead = head.get();
            if (head.compareAndSet(oldHead, newHead)) {
                tail.set(newHead);
                break;
            }
        }

        int count = 0;
        var segment = oldHead;
        while (segment != null) {
            for (int index = segment.getDequeIdx(); index < segment.getEnqueueIdx() && index < segment.capacity(); ++index) {
                final var getResult = segment.tryDeque(index);
                if (getResult != null) {
                    c.add(getResult.orElse(null));
                }
            }
            segment = segment.getNext();
        }
        return count;
    }

    @Override
    public int drainTo(Collection<? super ITEM> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        while (true) {
            consumeCpu();
            final var currentHead = head.get();
            if (currentHead.isEmpty()) {
                final var next = currentHead.getNext();
                if (next == null) {
                    return true;
                }
                head.compareAndSet(currentHead, next);
            } else {
                return false;
            }
        }
    }

    /**
     * Makes single try to enqueue Item,
     * MUST be called with acquired lock only
     *
     * @return true if enqueued successfully, false otherwise.
     */
    private boolean tryOffer(final ITEM item) {
        consumeCpu();
        final var currentTail = tail.get();
        final int enqueuePosition = currentTail.getAndIncEnqueueIdx();
        if (enqueuePosition >= currentTail.capacity()) {
            final var next = currentTail.getNext();
            if (next != null) {
                tail.compareAndSet(currentTail, next);
                return false;
            }
            final var newTail = new Segment<>(segmentSize, item);
            if (!currentTail.trySetNext(newTail)) {
                return false;
            }
            tail.compareAndSet(currentTail, newTail);
            size.getAndIncrement();
            return true;
        } else {
            final boolean success = currentTail.tryEnqueue(enqueuePosition, item);
            if (success) {
                size.getAndIncrement();
            }
            return success;
        }
    }

    /**
     * Makes single try to deque Item,
     * MUST be called with acquired lock only
     *
     * @return null if poll failed, Optional.empty() if there is null value, Optional.of() otherwise
     */
    private Optional<ITEM> tryPoll() {
        consumeCpu();
        final var currentHead = head.get();
        final int dequePosition = currentHead.getAndIncDequeIdx();
        if (dequePosition >= currentHead.capacity()) {
            currentHead.getAndDecDequeIdx();
            final var next = currentHead.getNext();
            if (next == null) {
                return null;
            }
            head.compareAndSet(currentHead, next);
            return null;
        } else {
            final var success = currentHead.tryDeque(dequePosition);
            if (success != null) {
                size.getAndDecrement();
            }
            return success;
        }
    }

    /**
     * Makes single try to peek Item,
     * MUST be called with acquired lock only
     *
     * @return null if peek failed, Optional.empty() if there is null value, Optional.of() otherwise
     */
    private Optional<ITEM> tryPeek() {
        consumeCpu();
        final var currentHead = head.get();
        final int dequePosition = currentHead.getDequeIdx();
        if (dequePosition >= currentHead.capacity()) {
            final var next = currentHead.getNext();
            if (next == null) {
                return null;
            }
            head.compareAndSet(currentHead, next);
            return null;
        } else {
            final var result = currentHead.tryGet(dequePosition);
            if (result == null) {
                currentHead.tryIncDequeIdx(dequePosition);
            }
            return result;
        }
    }

    private void consumeCpu() {
        if (cpuConsuming != 0) {
            Blackhole.consumeCPU(cpuConsuming);
        }
    }
}
