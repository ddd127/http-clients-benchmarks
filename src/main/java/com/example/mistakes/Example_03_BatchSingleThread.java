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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
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
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 7, time = 10)
public class Example_03_BatchSingleThread {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param(value = {"8"})
        private int threads;
        private HttpClient client;

        @Setup(Level.Trial)
        public void setup() {
            client = HttpClient.newBuilder()
                    .executor(Executors.newFixedThreadPool(4))
                    .build();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            ((ExecutorService) client.executor().get()).shutdownNow();
        }

        public HttpClient getClient() {
            return client;
        }
    }


    private List<HttpResponse<byte[]>> benchmark(final BenchmarkState benchmarkState,
                                                 final int batchSize) throws Exception {
        final List<CompletableFuture<HttpResponse<byte[]>>> futures = new ArrayList<>(batchSize);
        final HttpClient client = benchmarkState.getClient();
        final List<HttpResponse<byte[]>> result = new ArrayList<>();

        for (int i = 0; i < batchSize; ++i) {
            final HttpRequest request = HttpRequest
                    .newBuilder(URI.create("http://localhost:8080/do_request"))
                    .GET()
                    .build();
            futures.add(client.sendAsync(request, BodyHandlers.ofByteArray()));
        }

        for (final var future : futures) {
            result.add(future.get());
        }
        return result;
    }


    @Benchmark
    @OperationsPerInvocation(4)
    public List<HttpResponse<byte[]>> benchmark_batch_4(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 4);
    }

    @Benchmark
    @OperationsPerInvocation(16)
    public List<HttpResponse<byte[]>> benchmark_batch_16(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 16);
    }

    @Benchmark
    @OperationsPerInvocation(64)
    public List<HttpResponse<byte[]>> benchmark_batch_64(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 64);
    }

    @Benchmark
    @OperationsPerInvocation(256)
    public List<HttpResponse<byte[]>> benchmark_batch_256(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 256);
    }
}
