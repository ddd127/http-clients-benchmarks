package com.example.benchmark.java.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ThreadPoolVarHandleUtils {

    private ThreadPoolVarHandleUtils() {
        throw new IllegalArgumentException();
    }

    private static final VarHandle MAIN_LOCK_FIELD_HANDLE;
    private static final VarHandle WORKERS_FIELD_HANDLE;
    private static final VarHandle THREAD_FIELD_HANDLE;

    static {
        final MethodHandles.Lookup caller = MethodHandles.lookup();
        try {
            final var executorLookup = MethodHandles.privateLookupIn(ThreadPoolExecutor.class, caller);
            MAIN_LOCK_FIELD_HANDLE = executorLookup.findVarHandle(ThreadPoolExecutor.class, "mainLock",
                    ReentrantLock.class);
            WORKERS_FIELD_HANDLE = executorLookup.findVarHandle(ThreadPoolExecutor.class, "workers", HashSet.class);

            final Class<?> workerClass = Class.forName("java.util.concurrent.ThreadPoolExecutor$Worker");
            final var workerLookup = MethodHandles.privateLookupIn(workerClass, caller);
            THREAD_FIELD_HANDLE = workerLookup.findVarHandle(workerClass, "thread", Thread.class);
        } catch (IllegalAccessException | NoSuchFieldException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Thread> extractWorkers(final ThreadPoolExecutor executor) {
        final ReentrantLock lock = (ReentrantLock) MAIN_LOCK_FIELD_HANDLE.get(executor);
        lock.lock();
        try {
            return ((HashSet<?>) WORKERS_FIELD_HANDLE.get(executor)).stream()
                    .map(worker -> (Thread) THREAD_FIELD_HANDLE.get(worker))
                    .collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        final var executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
        executorService.prestartAllCoreThreads();
        final var workers = extractWorkers(executorService);
        for (final Thread worker : workers) {
            System.out.println(worker.getName());
        }
    }
}
