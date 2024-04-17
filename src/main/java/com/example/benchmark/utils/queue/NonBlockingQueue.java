package com.example.benchmark.utils.queue;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class NonBlockingQueue<ITEM> extends AbstractQueue<ITEM> implements BlockingQueue<ITEM> {

    private static final int DEFAULT_SEGMENT_SIZE = 8;
    private final AtomicReference<Segment<ITEM>> head;
    private final AtomicReference<Segment<ITEM>> tail;
    private final AtomicInteger size;
    private final AtomicInteger state;
    private final int segmentSize;

    public NonBlockingQueue(final int segmentSize) {
        this.segmentSize = segmentSize;
        size = new AtomicInteger(0);
        state = new AtomicInteger(0);
        final Segment<ITEM> initNode = new Segment<>(this.segmentSize);
        head = new AtomicReference<>(initNode);
        tail = new AtomicReference<>(initNode);
    }

    public NonBlockingQueue() {
        this(DEFAULT_SEGMENT_SIZE);
    }

    @Override
    public void put(ITEM t) throws InterruptedException {
        while (true) {
            lightLockWait();
            try {
                if (tryOffer(t)) {
                    return;
                }
            } finally {
                lightLockRelease();
            }
        }
    }

    @Override
    public boolean offer(ITEM t) {
        while (true) {
            lightLockWait();
            try {
                if (tryOffer(t)) {
                    return true;
                }
            } finally {
                lightLockRelease();
            }
        }
    }

    @Override
    public boolean offer(ITEM t, long timeout, TimeUnit unit) throws InterruptedException {
        final long endNanos = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < endNanos) {
            if (!lightLockTimeout(endNanos)) {
                return false;
            }
            try {
                if (tryOffer(t)) {
                    return true;
                }
            } finally {
                lightLockRelease();
            }
        }
        return false;
    }

    @Override
    public ITEM take() throws InterruptedException {
        while (true) {
            lightLockWait();
            try {
                final var pollResult = tryPoll();
                if (pollResult == null) {
                    continue;
                }
                return pollResult.orElse(null);
            } finally {
                lightLockRelease();
            }
        }
    }

    @Override
    public ITEM poll() {
        while (!isEmpty()) {
            lightLockWait();
            try {
                final var pollResult = tryPoll();
                if (pollResult != null) {
                    return pollResult.orElse(null);
                }
            } finally {
                lightLockRelease();
            }
        }
        return null;
    }

    @Override
    public ITEM poll(long timeout, TimeUnit unit) throws InterruptedException {
        final long endNanos = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() < endNanos) {
            if (!lightLockTimeout(endNanos)) {
                return null;
            }
            try {
                final var pollResult = tryPoll();
                if (pollResult == null) {
                    continue;
                }
                return pollResult.orElse(null);
            } finally {
                lightLockRelease();
            }
        }
        return null;
    }

    @Override
    public ITEM peek() {
        while (!isEmpty()) {
            lightLockWait();
            try {
                final var peekResult = tryPeek();
                if (peekResult != null) {
                    return peekResult.orElse(null);
                }
            } finally {
                lightLockRelease();
            }
        }
        return null;
    }

    @Override
    public Iterator<ITEM> iterator() {
        final var c = new ArrayList<ITEM>();
        fullLockWait();
        try {
            var segment = head.get();
            while (segment != null) {
                head.set(segment);
                for (int index = segment.getDequeIdx(); index < segment.getEnqueueIdx(); ++index) {
                    final var getResult = segment.tryGet(index);
                    if (getResult != null) {
                        c.add(getResult.orElse(null));
                    }
                }
                segment = segment.getNext();
            }
        } finally {
            fullLockRelease();
        }
        return c.iterator();
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
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super ITEM> c, int maxElements) {
        if (c == this) {
            throw new IllegalArgumentException();
        }
        fullLockWait();
        int count = 0;
        try {
            var segment = head.get();
            while (segment != null && count < maxElements) {
                head.set(segment);
                for (int index = segment.getDequeIdx(); index < segment.getEnqueueIdx(); ++index) {
                    final var getResult = segment.tryDeque(index);
                    if (getResult != null) {
                        c.add(getResult.orElse(null));
                        if (++count == maxElements) {
                            break;
                        }
                    }
                }
                segment = segment.getNext();
            }
        } finally {
            fullLockRelease();
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        while (true) {
            lightLockWait();
            try {
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
            } finally {
                lightLockRelease();
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
        final var currentTail = tail.get();
        final int enqueuePosition = currentTail.getAndIncEnqueueIdx();
        if (enqueuePosition >= currentTail.capacity()) {
            final var next = currentTail.getNext();
            if (next != null) {
                tail.compareAndSet(currentTail, next);
                return false;
            }
            final var newTail = new Segment<ITEM>(segmentSize, item);
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
        final var currentHead = head.get();
        final int dequePosition = currentHead.getAndIncDequeIdx();
        if (dequePosition >= currentHead.capacity()) {
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

    private void lightLockWait() {
        while (true) {
            if (lightLockTry()) {
                return;
            }
        }
    }

    private boolean lightLockTimeout(final long endNanos) {
        while (System.nanoTime() < endNanos) {
            if (lightLockTry()) {
                return true;
            }
        }
        return false;
    }

    private boolean lightLockTry() {
        if (state.get() < 0) {
            return false; // someone holds full lock
        }
        final int value = state.getAndIncrement();
        if (value >= 0) {
            // light lock acquired, OK
            return true;
        } else {
            // someone holds full lock, decrement and exit
            state.getAndDecrement();
            return false;
        }
    }

    private void lightLockRelease() {
        state.getAndDecrement();
    }

    private void fullLockWait() {
        while (true) {
            if (fullLockTry()) {
                return;
            }
        }
    }

    private boolean fullLockTry() {
        while (true) {
            int initial = state.get();
            if (initial < 0) {
                return false; // someone else holds full lock
            }
            if (!state.compareAndSet(initial, initial - Integer.MAX_VALUE)) {
                continue; // state changed, try again
            }
            // set state to negative, wait light locks release
            while (true) {
                if (state.get() == -Integer.MAX_VALUE) {
                    return true;
                }
            }
        }
    }

    private void fullLockRelease() {
        if (!state.compareAndSet(-Integer.MAX_VALUE, 0)) {
            throw new IllegalStateException("Trying to release non-acquired lock");
        }
    }
}
