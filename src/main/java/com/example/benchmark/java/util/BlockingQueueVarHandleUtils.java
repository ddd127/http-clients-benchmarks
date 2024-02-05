package com.example.benchmark.java.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingQueueVarHandleUtils {

    private BlockingQueueVarHandleUtils() {
        throw new IllegalArgumentException();
    }

    private static final VarHandle TAKE_LOCK_HANDLE;
    private static final VarHandle SYNC_HANDLE;
    private static final VarHandle EXCLUSIVE_OWNER_HANDLE;
    private static final VarHandle STATE_HANDLE;

    static {
        final MethodHandles.Lookup caller = MethodHandles.lookup();
        try {
            final MethodHandles.Lookup queueLookup = MethodHandles.privateLookupIn(LinkedBlockingQueue.class, caller);
            TAKE_LOCK_HANDLE = queueLookup.findVarHandle(LinkedBlockingQueue.class, "takeLock", ReentrantLock.class);

            final MethodHandles.Lookup lockLookup = MethodHandles.privateLookupIn(ReentrantLock.class, caller);
            final Class<?> syncClass = Class.forName("java.util.concurrent.locks.ReentrantLock$Sync");
            SYNC_HANDLE = lockLookup.findVarHandle(ReentrantLock.class, "sync", syncClass);

            final MethodHandles.Lookup ownableSynchronizerLookup = MethodHandles.privateLookupIn(AbstractOwnableSynchronizer.class, caller);
            EXCLUSIVE_OWNER_HANDLE = ownableSynchronizerLookup
                    .findVarHandle(AbstractOwnableSynchronizer.class, "exclusiveOwnerThread", Thread.class);

            final MethodHandles.Lookup queuedSynchronizerLookup = MethodHandles.privateLookupIn(AbstractQueuedSynchronizer.class, caller);
            STATE_HANDLE = queuedSynchronizerLookup
                    .findVarHandle(AbstractQueuedSynchronizer.class, "state", int.class);

        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static AbstractQueuedSynchronizer extractSynchronizer(final BlockingQueue<?> queue) {
        final ReentrantLock lock = (ReentrantLock) TAKE_LOCK_HANDLE.get(queue);
        return (AbstractQueuedSynchronizer) SYNC_HANDLE.get(lock);
    }

    public static String extractTakeLockOwnerName(final AbstractOwnableSynchronizer lock) {
        final Thread owner = (Thread) EXCLUSIVE_OWNER_HANDLE.getVolatile(lock);
        return owner == null ? null : owner.getName();
    }

    public static int extractTakeLockState(final AbstractQueuedSynchronizer lock) {
        return (int) STATE_HANDLE.getVolatile(lock);
    }

    public static void main(String[] args) {
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        final var queue = ((ThreadPoolExecutor) executorService).getQueue();
        final var lock = extractSynchronizer(queue);
        final var name = extractTakeLockOwnerName(lock);
        System.out.println(name);
        final Map<String, Integer> fuck = new HashMap<>();
        fuck.put(null, 1);
        fuck.put("a", 2);
        System.out.println(fuck.get(null));
        System.out.println(fuck.get("a"));
    }
}
