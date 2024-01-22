package com.example.mistakes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 7, time = 10)
public class Example_05_ParallelizeSingleThread {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param(value = {"8"})
        private int threads;
        @Param(value = {"4", "16", "64", "128"})
        private int parallelism;
        private HttpClient client;
        private List<CompletableFuture<HttpResponse<byte[]>>> futures;

        @Setup(Level.Trial)
        public void setup() {
            client = HttpClient.newBuilder()
                    .executor(Executors.newFixedThreadPool(4))
                    .build();
            futures = Stream.<CompletableFuture<HttpResponse<byte[]>>>generate(() -> null)
                    .limit(parallelism)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            ((ExecutorService) client.executor().get()).shutdownNow();
        }

        public HttpClient getClient() {
            return client;
        }

        public List<CompletableFuture<HttpResponse<byte[]>>> getFutures() {
            return futures;
        }
    }


    @Benchmark
    public HttpResponse<byte[]> benchmark(final BenchmarkState benchmarkState) throws Exception {
        final var client = benchmarkState.getClient();
        final var futures = benchmarkState.getFutures();

        while (true) {
            for (int i = 0; i < futures.size(); ++i) {
                final var future = futures.get(i);
                if (future != null && !future.isDone()) {
                    continue;
                }

                final HttpRequest request = HttpRequest
                        .newBuilder(URI.create("http://localhost:8080/do_request"))
                        .GET()
                        .build();
                futures.set(i, client.sendAsync(request, BodyHandlers.ofByteArray()));

                if (future != null) {
                    return future.get();
                }
            }
        }
    }
}
