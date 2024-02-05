package com.example.benchmark.java;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.example.benchmark.java.util.BlockingQueueVarHandleUtils;
import com.example.benchmark.java.util.ThreadPoolVarHandleUtils;
import com.example.benchmark.java.util.ThreadVarHandeUtils;
import com.example.client.ClientAdapter;
import com.example.client.impl.JavaClientAdapter;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@SuppressWarnings("Duplicates")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 20)
@Measurement(iterations = 6, time = 20)
public class JavaClientAnalysisBenchmark {

    private static final String URL = "http://localhost:8080/do_request";

    @State(Scope.Benchmark)
    public static class ClientState {

        @Param(value = {
                "1",
                "2",
        })
        private int clientsCount;
        @Param(value = {
                "2",
                "4",
                "8",
                "12",
        })
        private int ioThreads;
        @Param(value = {
                "0",
//                "524288",
        })
        private int bodySize;

        private ExecutorService executor;
        private List<ClientAdapter<?, ?>> clients;
        private byte[] body;

//        private BlockingQueue<Runnable> executorQueue;
//        private int[] queueSizeDistribution;
//
//        private AbstractQueuedSynchronizer takeLockSynchronizer;
//        private Map<String, Integer> lockOwners;
//        private Map<Integer, Integer> lockStates;
//
//        private List<Thread> workerThreads;
//        private EnumMap<Thread.State, Integer> workerStates;
//
//        private Map<String, Integer> workerParkBlockerClasses;
////        private Map<String, Integer> blockerOwnerClasses;
//        private int parkBlockerIsTakeLockSync;
//
//        private Thread monitorThread;

        @Setup(Level.Trial)
        public void setup() {
            executor = new ForkJoinPool(ioThreads);
//            executor = Executors.newFixedThreadPool(ioThreads);
//            ((ThreadPoolExecutor) executor).prestartAllCoreThreads();
            clients = IntStream.range(0, clientsCount)
                    .mapToObj((__) -> new JavaClientAdapter(executor))
                    .collect(Collectors.toList());
            if (bodySize == 0) {
                body = null;
            } else {
                body = new byte[bodySize];
                ThreadLocalRandom.current().nextBytes(body);
            }

//            executorQueue = ((ThreadPoolExecutor) executor).getQueue();
//            queueSizeDistribution = new int[4096];
//
//            takeLockSynchronizer = BlockingQueueVarHandleUtils.extractSynchronizer(executorQueue);
//            lockOwners = new HashMap<>();
//            lockStates = new HashMap<>();
//
//            workerThreads = ThreadPoolVarHandleUtils.extractWorkers((ThreadPoolExecutor) executor);
//            workerStates = new EnumMap<>(Thread.State.class);
//
//            workerParkBlockerClasses = new HashMap<>();
//            parkBlockerIsTakeLockSync = 0;
//
//            monitorThread = new Thread(() -> {
//                long prevTime = System.nanoTime();
//                while (!Thread.currentThread().isInterrupted()) {
//                    final long time = System.nanoTime();
//                    if (time - prevTime < 1_000_000) continue;
//
//                    final int queueSize = executorQueue.size();
//                    ++queueSizeDistribution[queueSize];
//
//                    final String lockOwner = BlockingQueueVarHandleUtils.extractTakeLockOwnerName(takeLockSynchronizer);
//                    lockOwners.merge(lockOwner, 1, Integer::sum);
//
//                    final int lockState = BlockingQueueVarHandleUtils.extractTakeLockState(takeLockSynchronizer);
//                    lockStates.merge(lockState, 1, Integer::sum);
//
//                    for (final Thread worker : workerThreads) {
//
//                        final var state = worker.getState();
//                        workerStates.merge(state, 1, Integer::sum);
//
//                        final var parkBlocker = ThreadVarHandeUtils.extractParkBlocker(worker);
//                        final var parkBlockerClassName = parkBlocker == null ? null : parkBlocker.getClass().getName();
//                        workerParkBlockerClasses.merge(parkBlockerClassName, 1, Integer::sum);
//
//                        if (parkBlocker == takeLockSynchronizer) {
//                            ++parkBlockerIsTakeLockSync;
//                        }
//                    }
//
//                    prevTime = time;
//                }
//            });
//
//            monitorThread.start();
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
//            monitorThread.interrupt();
//            monitorThread.join();
//            monitorThread = null;

            executor.shutdownNow();
            executor = null;
            clients = null;
            body = null;

//            // queue size & related
//            System.out.println();
//            System.out.println("Queue size distribution:");
//            System.out.print("[  ");
//            for (int i = 0; i < queueSizeDistribution.length; ++i) {
//                if (i != 0) {
//                    System.out.print(", ");
//                }
//                System.out.print("[" + i + ", " + queueSizeDistribution[i] + "]");
//            }
//            System.out.println("  ]");
//            executorQueue = null;
//            queueSizeDistribution = null;
//
//            // locks & related
//            System.out.println();
//            System.out.println("Lock owner info:");
//            boolean first = true;
//            System.out.print("{  ");
//            for (final Map.Entry<String, Integer> ownerToCount :
//                    lockOwners.entrySet().stream()
//                            .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed())
//                            .toList()) {
//                if (first) {
//                    first = false;
//                } else {
//                    System.out.print(", ");
//                }
//                System.out.printf("'%s': %d", ownerToCount.getKey(), ownerToCount.getValue());
//            }
//            System.out.println("  }");
//            lockOwners = null;
//            System.out.println();
//            System.out.println("Lock states info:");
//            first = true;
//            System.out.print("{  ");
//            for (final Map.Entry<Integer, Integer> stateToCount :
//                    lockStates.entrySet().stream()
//                            .sorted(Comparator.comparing(Map.Entry<Integer, Integer>::getValue).reversed())
//                            .toList()) {
//                if (first) {
//                    first = false;
//                } else {
//                    System.out.print(", ");
//                }
//                System.out.printf("'%d': %d", stateToCount.getKey(), stateToCount.getValue());
//            }
//            System.out.println("  }");
//            lockStates = null;
//            takeLockSynchronizer = null;
//
//            // thread states & related
//            System.out.println();
//            System.out.println("Worker states info:");
//            first = true;
//            System.out.print("{  ");
//            for (final Map.Entry<Thread.State, Integer> stateToCount :
//                    workerStates.entrySet().stream()
//                            .sorted(Comparator.comparing(Map.Entry<Thread.State, Integer>::getValue).reversed())
//                            .toList()) {
//                if (first) {
//                    first = false;
//                } else {
//                    System.out.print(", ");
//                }
//                System.out.printf("'%s': %d", stateToCount.getKey().name(), stateToCount.getValue());
//            }
//            System.out.println("  }");
//            workerStates = null;
//
//            // thread states & related
//            System.out.println();
//            System.out.println("Worker park blockers:");
//            first = true;
//            System.out.print("{  ");
//            for (final Map.Entry<String, Integer> blockerClassToCount :
//                    workerParkBlockerClasses.entrySet().stream()
//                            .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed())
//                            .toList()) {
//                if (first) {
//                    first = false;
//                } else {
//                    System.out.print(", ");
//                }
//                System.out.printf("'%s': %d", blockerClassToCount.getKey(), blockerClassToCount.getValue());
//            }
//            System.out.println("  }");
//            workerParkBlockerClasses = null;
//
//            System.out.println("Park blocker is takeLock Sync");
//            System.out.println(parkBlockerIsTakeLockSync);
//            parkBlockerIsTakeLockSync = 0;
//
//            workerThreads = null;
//
//            System.out.println();
        }

        public List<ClientAdapter<?, ?>> getClients() {
            return clients;
        }

        public byte[] getBody() {
            return body;
        }
    }

    @State(Scope.Thread)
    public static class CommonThreadState {

        public int getProducerThreads() {
            throw new IllegalStateException("Must be defined by subclass");
        }

        @Param(value = {
                "256",
        })
        private int parallelism;

        private List<Future<?>> futures;

        @Setup(Level.Iteration)
        public void setup() {
            final int futuresSize = parallelism / getProducerThreads();
            if (futuresSize <= 0) {
                throw new IllegalStateException("Future size must be positive, but found " + futuresSize);
            }
            futures = Stream.<CompletableFuture<?>>generate(() -> null)
                    .limit(parallelism / getProducerThreads())
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        public List<Future<?>> getFutures() {
            return futures;
        }
    }

    private ClientResponse iteration(final ClientState clientState,
                                     final CommonThreadState threadState) throws Exception {
        final var clients = clientState.getClients();
        final var futures = threadState.getFutures();

        while (true) {
            for (int i = 0; i < futures.size(); ++i) {
                final var client = clients.get(i % clients.size());
                final var future = futures.get(i);

                if (future != null && !future.isDone()) {
                    continue;
                }

                final byte[] body = clientState.getBody();
                final Object request = client.mapRequest(new ClientRequest(URL, body));
                futures.set(i, client.sendUnchecked(request));

                if (future != null) {
                    try {
                        return client.mapResponseUnchecked(future.get());
                    } catch (Exception e) {
                        System.err.println(
                                "Got exception, class = '" + e.getClass().getName() + "', message = '" + e.getMessage() + "'"
                        );
                    }
                }
            }
        }
    }


    // 1 thread-producer

    public static class ThreadState_Producer_1 extends CommonThreadState {
        @Override
        public int getProducerThreads() {
            return 1;
        }
    }

    @Benchmark
    @Threads(1)
    public ClientResponse benchmark_producer_1(final ClientState clientState,
                                               final ThreadState_Producer_1 threadState) throws Exception {
        return iteration(clientState, threadState);
    }
}
