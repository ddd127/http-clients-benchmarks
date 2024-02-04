package com.example.benchmark.java;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        })
        private int bodySize;

        private ExecutorService executor;
        private List<ClientAdapter<?, ?>> clients;
        private byte[] body;

        @Setup(Level.Trial)
        public void setup() {
            executor = Executors.newFixedThreadPool(ioThreads);
            clients = IntStream.range(0, clientsCount)
                    .mapToObj((__) -> new JavaClientAdapter(executor))
                    .collect(Collectors.toList());
            if (bodySize == 0) {
                body = null;
            } else {
                body = new byte[bodySize];
                ThreadLocalRandom.current().nextBytes(body);
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            executor.shutdownNow();
            executor = null;
            clients = null;
            body = null;
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
