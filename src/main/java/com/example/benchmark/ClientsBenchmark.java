package com.example.benchmark;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 15)
@Measurement(iterations = 3, time = 10)
public class ClientsBenchmark {

    private static final String URL = "http://localhost:8080/do_request";

    @State(Scope.Benchmark)
    public static class ClientState {

        @Param(value = {
                "BASELINE_CLIENT",
                "JAVA_CLIENT",
                "ASYNC_CLIENT",
                "APACHE_CLIENT",
                "JETTY_CLIENT",
        })
        private String clientName;
        @Param(value = {
                "1",
                "2",
                "4",
                "8",
                "16",
        })
        private int ioThreads;
        @Param(value = {
                "0",
                "1024",
                "8192",
                "65536",
                "524288",
        })
        private int bodySize;

        private ClientAdapter<?, ?> client;

        @Setup(Level.Trial)
        public void setup() {
            client = AdaptedClient.create(clientName, new ClientConfiguration(ioThreads));
        }

        @TearDown
        public void tearDown() throws Exception {
            client.shutdown();
        }

        public ClientAdapter<?, ?> getClient() {
            return client;
        }

        public int getBodySize() {
            return bodySize;
        }
    }

    @State(Scope.Thread)
    public static class CommonThreadState {

        @Param(value = {
                "32",
                "64",
                "128",
                "256",
                "512",
        })
        private int parallelism;
        public int getProducerThreads() {
            throw new IllegalStateException("Must be defined by subclass");
        }

        private List<Future<?>> futures;

        private ThreadLocalRandom random;

        @Setup(Level.Iteration)
        public void setup() {
            final int futuresSize = parallelism / getProducerThreads();
            if (futuresSize <= 0) {
                throw new IllegalStateException("Future size must be positive, but found " + futuresSize);
            }
            futures = Stream.<CompletableFuture<?>>generate(() -> null)
                    .limit(parallelism / getProducerThreads())
                    .collect(Collectors.toCollection(ArrayList::new));
            random = ThreadLocalRandom.current();
        }

        public List<Future<?>> getFutures() {
            return futures;
        }

        public byte[] generateBodyOrNull(final int bodySize) {
            if (bodySize == 0) {
                return null;
            }
            final byte[] body = new byte[bodySize];
            random.nextBytes(body);
            return body;
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

                final byte[] body = threadState.generateBodyOrNull(clientState.getBodySize());
                final Object request = client.mapRequest(new ClientRequest(URL, body));
                futures.set(i, client.sendUnchecked(request));

                if (future != null) {
                    return client.mapResponseUnchecked(future.get());
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


    // 4 threads-producers

    public static class ThreadState_Producer_4 extends CommonThreadState {
        @Override
        public int getProducerThreads() {
            return 4;
        }
    }

    @Benchmark
    @Threads(4)
    public ClientResponse benchmark_producer_4(final ClientState clientState,
                                               final ThreadState_Producer_4 threadState) throws Exception {
        return iteration(clientState, threadState);
    }


    // 8 threads-producers

    public static class ThreadState_Producer_8 extends CommonThreadState {
        @Override
        public int getProducerThreads() {
            return 8;
        }
    }

    @Benchmark
    @Threads(8)
    public ClientResponse benchmark_producer_8(final ClientState clientState,
                                               final ThreadState_Producer_8 threadState) throws Exception {
        return iteration(clientState, threadState);
    }
}
