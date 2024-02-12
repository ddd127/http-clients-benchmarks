package com.example.benchmark.utils.queue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

class Segment<ITEM> {
    private static final Object FREE = new Object();
    private static final Object DONE = new Object();

    private final AtomicReferenceArray<Object> items;
    private final AtomicReference<Segment<ITEM>> next;
    private final AtomicInteger enqueueIdx;
    private final AtomicInteger dequeIdx;

    Segment(final int size) {
        items = new AtomicReferenceArray<>(size);
        for (int i = 0; i < size; ++i) {
            items.set(i, FREE);
        }
        next = new AtomicReference<>(null);
        enqueueIdx = new AtomicInteger(0);
        dequeIdx = new AtomicInteger(0);
    }

    Segment(final int size, final ITEM item) {
        this(size);
        items.set(0, item);
        enqueueIdx.incrementAndGet();
    }

    boolean tryEnqueue(final int index, final ITEM value) {
        return items.compareAndSet(index, FREE, value);
    }

    Optional<ITEM> tryDeque(final int index) {
        final var result = items.getAndSet(index, DONE);
        if (result != FREE && result != DONE) {
            //noinspection unchecked
            return Optional.ofNullable((ITEM) result);
        } else {
            return null;
        }
    }

    Optional<ITEM> tryGet(final int index) {
        final var result = items.get(index);
        if (result != FREE && result != DONE) {
            //noinspection unchecked
            return Optional.ofNullable((ITEM) result);
        } else {
            return null;
        }
    }

    boolean trySetNext(final Segment<ITEM> segment) {
        return next.compareAndSet(null, segment);
    }

    Segment<ITEM> getNext() {
        return next.get();
    }

    int getEnqueueIdx() {
        return enqueueIdx.get();
    }

    int getAndIncEnqueueIdx() {
        return enqueueIdx.getAndIncrement();
    }

    int getDequeIdx() {
        return dequeIdx.get();
    }

    int getAndIncDequeIdx() {
        return dequeIdx.getAndIncrement();
    }

    boolean tryIncDequeIdx(int expected) {
        return dequeIdx.compareAndSet(expected, expected + 1);
    }

    boolean isEmpty() {
        return dequeIdx.get() >= enqueueIdx.get() || dequeIdx.get() >= items.length();
    }

    int capacity() {
        return items.length();
    }
}

