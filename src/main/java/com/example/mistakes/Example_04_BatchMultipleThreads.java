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
import org.openjdk.jmh.annotations.Fork;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@SuppressWarnings("Duplicates")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 4, time = 30)
@Measurement(iterations = 16, time = 30)
public class Example_04_BatchMultipleThreads {

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
    @Threads(2)
    @OperationsPerInvocation(4)
    public List<HttpResponse<byte[]>> benchmark_batch_4_threads_2(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 4);
    }

    @Benchmark
    @Threads(2)
    @OperationsPerInvocation(16)
    public List<HttpResponse<byte[]>> benchmark_batch_16_threads_2(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 16);
    }

    @Benchmark
    @Threads(2)
    @OperationsPerInvocation(64)
    public List<HttpResponse<byte[]>> benchmark_batch_64_threads_2(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 64);
    }

    @Benchmark
    @Threads(2)
    @OperationsPerInvocation(256)
    public List<HttpResponse<byte[]>> benchmark_batch_256_threads_2(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 256);
    }


    @Benchmark
    @Threads(4)
    @OperationsPerInvocation(4)
    public List<HttpResponse<byte[]>> benchmark_batch_4_threads_4(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 4);
    }

    @Benchmark
    @Threads(4)
    @OperationsPerInvocation(16)
    public List<HttpResponse<byte[]>> benchmark_batch_16_threads_4(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 16);
    }

    @Benchmark
    @Threads(4)
    @OperationsPerInvocation(64)
    public List<HttpResponse<byte[]>> benchmark_batch_64_threads_4(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 64);
    }

    @Benchmark
    @Threads(4)
    @OperationsPerInvocation(256)
    public List<HttpResponse<byte[]>> benchmark_batch_256_threads_4(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 256);
    }


    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(4)
    public List<HttpResponse<byte[]>> benchmark_batch_4_threads_8(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 4);
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(16)
    public List<HttpResponse<byte[]>> benchmark_batch_16_threads_8(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 16);
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(64)
    public List<HttpResponse<byte[]>> benchmark_batch_64_threads_8(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 64);
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(256)
    public List<HttpResponse<byte[]>> benchmark_batch_256_threads_8(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 256);
    }


    @Benchmark
    @Threads(16)
    @OperationsPerInvocation(4)
    public List<HttpResponse<byte[]>> benchmark_batch_4_threads_16(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 4);
    }

    @Benchmark
    @Threads(16)
    @OperationsPerInvocation(16)
    public List<HttpResponse<byte[]>> benchmark_batch_16_threads_16(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 16);
    }

    @Benchmark
    @Threads(16)
    @OperationsPerInvocation(64)
    public List<HttpResponse<byte[]>> benchmark_batch_64_threads_16(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 64);
    }

    @Benchmark
    @Threads(16)
    @OperationsPerInvocation(256)
    public List<HttpResponse<byte[]>> benchmark_batch_256_threads_16(final BenchmarkState benchmarkState) throws Exception {
        return benchmark(benchmarkState, 256);
    }
}
