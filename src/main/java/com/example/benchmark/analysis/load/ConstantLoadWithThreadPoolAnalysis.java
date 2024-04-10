package com.example.benchmark.analysis.load;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.example.benchmark.Utils;
import com.example.client.AdaptedClient;
import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.impl.AsyncClientAdapter;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;
import org.asynchttpclient.ListenableFuture;
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
import org.openjdk.jmh.annotations.Warmup;

@SuppressWarnings("Duplicates")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 2, time = 30)
@Measurement(iterations = 12, time = 20)
public class ConstantLoadWithThreadPoolAnalysis {

    private static final String URL = Utils.SERVER_URL;

    @State(Scope.Benchmark)
    public static class ClientState {

        @Param(value = {
//                "BASELINE_CLIENT",
//                "JAVA_CLIENT",
                "ASYNC_CLIENT",
//                "APACHE_CLIENT",
        })
        private String clientName;
        @Param(value = {
                "2",
                "4",
                "8",
                "10",
                "12",
        })
        private int ioThreads;
        @Param(value = {
                "1",
                "2",
                "3",
                "4",
        })
        private int producerThreads;

        private AsyncClientAdapter client;
        private ExecutorService producerPool;

        @Setup(Level.Trial)
        public void setup() {
            if (!AdaptedClient.ASYNC_CLIENT.name().equals(clientName)) {
                throw new IllegalArgumentException("Wrong client name + '" + clientName + "'");
            }
            client = new AsyncClientAdapter(new ClientConfiguration(ioThreads));
            producerPool = Executors.newFixedThreadPool(producerThreads);
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            client.shutdown();
            client = null;
            producerPool.shutdownNow();
            producerPool = null;
        }

        public ClientAdapter<?, ?> getClient() {
            return client;
        }

        public ExecutorService getProducerPool() {
            return producerPool;
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {

        public int getProducerThreads() {
            throw new IllegalStateException("Must be defined by subclass");
        }

        @Param(value = {
                "64",
                "1024",
        })
        private int parallelism;

        private List<Future<ClientResponse>> futures;

        @Setup(Level.Iteration)
        public void setup() {
            futures = IntStream.range(0, parallelism)
                    .mapToObj(__ -> (Future<ClientResponse>) null)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        public List<Future<ClientResponse>> getFutures() {
            return futures;
        }
    }

    private ClientResponse iteration(final ClientState clientState,
                                     final ThreadState threadState) throws Exception {
        final var client = clientState.getClient();
        final var futures = threadState.getFutures();
        final var producerPool = clientState.getProducerPool();

        while (true) {
            for (int i = 0; i < futures.size(); ++i) {
                final var future = futures.get(i);
                if (future != null && !future.isDone()) {
                    continue;
                }

                final var newFuture = new CompletableFuture<ClientResponse>();

                producerPool.submit(() -> {
                    final Object request = client.mapRequest(new ClientRequest(URL, null));
                    final var requestCompletionFuture = (ListenableFuture<?>) client.sendUnchecked(request);
                    requestCompletionFuture.addListener(() -> {
                        try {
                            var response = client.mapResponseUnchecked(requestCompletionFuture.get());
                            newFuture.complete(response);
                        } catch (Exception e) {
                            newFuture.completeExceptionally(e);
                        }
                    }, producerPool);
                });

                futures.set(i, newFuture);

                if (future != null) {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        System.err.println(
                                "Got exception, class = '" + e.getClass().getName() + "', message = '" + e.getMessage() + "'"
                        );
                    }
                }
            }
        }
    }

    @Benchmark
    public ClientResponse benchmark(final ClientState clientState,
                                    final ThreadState threadState) throws Exception {
        return iteration(clientState, threadState);
    }
}
