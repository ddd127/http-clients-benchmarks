package com.example.benchmark.java;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.example.client.AdaptedClient;
import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
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
@Warmup(iterations = 2, time = 30)
@Measurement(iterations = 3, time = 20)
public class AsyncClientAnalysisBenchmark {

    private static final String URL = "http://localhost:8080/do_request";

    @State(Scope.Benchmark)
    public static class ClientState {

        @Param(value = {
//                "BASELINE_CLIENT",
//                "JAVA_CLIENT",
                "ASYNC_CLIENT",
//                "APACHE_CLIENT",
//                "JETTY_CLIENT",
        })
        private String clientName;
        @Param(value = {
                "2",
                "4",
                "8",
                "12",
        })
        private int ioThreads;
        @Param(value = {
                "0",
//                "1024",
//                "8192",
//                "65536",
//                "524288",
        })
        private int bodySize;

        private ClientAdapter<?, ?> client;
        private byte[] body;

        @Setup(Level.Trial)
        public void setup() {
            client = AdaptedClient.create(clientName, new ClientConfiguration(ioThreads));
            if (bodySize == 0) {
                body = null;
            } else {
                body = new byte[bodySize];
                ThreadLocalRandom.current().nextBytes(body);
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            client.shutdown();
            client = null;
            body = null;
        }

        public ClientAdapter<?, ?> getClient() {
            return client;
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
//                "32",
//                "64",
//                "128",
                "256",
//                "512",
        })
        private int parallelism;

        private Thread monitorThread;
        private int[] completedFuturesFrequency;

        private List<Future<?>> futures;

        @Setup(Level.Trial)
        public void setup() {
            final int futuresSize = parallelism / getProducerThreads();
            if (futuresSize <= 0) {
                throw new IllegalStateException("Future size must be positive, but found " + futuresSize);
            }
            futures = IntStream.range(0, parallelism / getProducerThreads())
                    .mapToObj(__ -> (CompletableFuture<?>) null)
                    .collect(Collectors.toCollection(ArrayList::new));
            completedFuturesFrequency = new int[parallelism + 1];
            monitorThread = new Thread(() -> {
                long prevTime = System.nanoTime();
                while (!Thread.interrupted()) {
                    final long time = System.nanoTime();
                    if (time - prevTime < 1_000_000) continue;

                    int inProgress = 0;
                    for (final var future : futures) {
                        if (future != null && future.isDone()) ++inProgress;
                    }
                    ++completedFuturesFrequency[inProgress];
                }
            });
            monitorThread.start();
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            monitorThread.interrupt();
            monitorThread.join();
            System.out.println("[  ");
            for (int i = 0; i < completedFuturesFrequency.length; ++i) {
                if (i != 0) {
                    System.out.print(", ");
                }
                System.out.printf("[%d, %d]", i, completedFuturesFrequency[i]);
            }
            System.out.println("  ]");
            System.out.println();
            completedFuturesFrequency = null;
            monitorThread = null;
        }

        public List<Future<?>> getFutures() {
            return futures;
        }
    }

    private ClientResponse iteration(final ClientState clientState,
                                     final CommonThreadState threadState) throws Exception {
        final var client = clientState.getClient();
        final var futures = threadState.getFutures();

        while (true) {
            for (int i = 0; i < futures.size(); ++i) {
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


    // 2 threads-producers

    public static class ThreadState_Producer_2 extends CommonThreadState {
        @Override
        public int getProducerThreads() {
            return 2;
        }
    }

    @Benchmark
    @Threads(2)
    public ClientResponse benchmark_producer_2(final ClientState clientState,
                                               final ThreadState_Producer_2 threadState) throws Exception {
        return iteration(clientState, threadState);
    }
}