package com.example.benchmark.java.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class ThreadVarHandeUtils {

    private static final VarHandle PARK_BLOCKER_HANDLE;

    static {
        final MethodHandles.Lookup caller = MethodHandles.lookup();
        try {
            final var executorLookup = MethodHandles.privateLookupIn(Thread.class, caller);
            PARK_BLOCKER_HANDLE = executorLookup.findVarHandle(Thread.class, "parkBlocker",
                    Object.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private ThreadVarHandeUtils() {
        throw new IllegalArgumentException();
    }

    public static Object extractParkBlocker(final Thread thread) {
        return PARK_BLOCKER_HANDLE.get(thread);
    }
}
