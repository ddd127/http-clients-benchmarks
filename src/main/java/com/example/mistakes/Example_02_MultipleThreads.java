package com.example.mistakes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
@Warmup(iterations = 4, time = 30)
@Measurement(iterations = 16, time = 30)
public class Example_02_MultipleThreads {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param(value = {"8"})
        private int threads;
        private HttpClient client;

        @Setup(Level.Trial)
        public void setup() {
            client = HttpClient.newBuilder()
                    .executor(Executors.newFixedThreadPool(threads))
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


    private HttpResponse<byte[]> benchmark(final BenchmarkState benchmarkState) throws Exception {
        final HttpRequest request = HttpRequest
                .newBuilder(URI.create("http://localhost:8080/do_request"))
                .GET()
                .build();
        final CompletableFuture<HttpResponse<byte[]>> future = benchmarkState.getClient()
                .sendAsync(request, BodyHandlers.ofByteArray());
        return future.get();
    }


    @Benchmark
    @Threads(2)
    public HttpResponse<byte[]> benchmark_threads_2(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState);
    }

    @Benchmark
    @Threads(4)
    public HttpResponse<byte[]> benchmark_threads_4(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState);
    }

    @Benchmark
    @Threads(8)
    public HttpResponse<byte[]> benchmark_threads_8(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState);
    }

    @Benchmark
    @Threads(16)
    public HttpResponse<byte[]> benchmark_threads_16(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState);
    }
}
