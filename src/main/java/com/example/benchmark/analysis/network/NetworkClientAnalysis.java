package com.example.benchmark.analysis.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.example.benchmark.Utils;
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
@Measurement(iterations = 12, time = 20)
public class NetworkClientAnalysis {

    private static final String URL = Utils.SERVER_URL;

    @State(Scope.Benchmark)
    public static class ClientState {

        @Param(value = {
//                "ASYNC_CLIENT",
                "BASELINE_CLIENT",
        })
        private String clientName;
        @Param(value = {
                "2",
                "6",
                "10",
                "14",
                "18",
                "22",
        })
        private int ioThreads;
        @Param(value = {
                "0",
//                "2048",
//                "8192",
//                "32768",
//                "131072",
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
                "32",
                "64",
                "128",
                "256",
                "512",
                "1024",
                "2048",
                "4096",
        })
        private int parallelism;

        private List<Future<?>> futures;

        @Setup(Level.Iteration)
        public void setup() {
            final int futuresSize = parallelism / getProducerThreads();
            if (futuresSize <= 0) {
                throw new IllegalStateException("Future size must be positive, but found " + futuresSize);
            }
            futures = IntStream.range(0, parallelism / getProducerThreads())
                    .mapToObj(__ -> (CompletableFuture<?>) null)
                    .collect(Collectors.toCollection(ArrayList::new));
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

    // 4 threads-producers

    public static class ThreadState_Producer_4 extends CommonThreadState {

        @Param(value = {
                "4",
        })
        private int producerThreads;

        @Override
        public int getProducerThreads() {
            return producerThreads;
        }
    }

    @Benchmark
    @Threads(4)
    public ClientResponse benchmark_producer_4(final ClientState clientState,
                                               final ThreadState_Producer_4 threadState) throws Exception {
        return iteration(clientState, threadState);
    }
}
