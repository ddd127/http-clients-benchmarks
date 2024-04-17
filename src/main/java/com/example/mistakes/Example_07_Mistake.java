package com.example.mistakes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
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
//@Warmup(iterations = 4, time = 30)
//@Measurement(iterations = 16, time = 30)
@Warmup(iterations = 2, time = 30)
@Measurement(iterations = 3, time = 20)
public class Example_07_Mistake {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param(value = {"0", "8192", "65536", "524288"})
        private int bodySize;
        private byte[] body;
        @Param(value = {"8"})
        private int threads;
        private HttpClient client;

        @Setup(Level.Trial)
        public void setup() {
            client = HttpClient.newBuilder()
                    .executor(Executors.newFixedThreadPool(threads))
                    .build();
            if (bodySize != 0) {
                body = new byte[bodySize];
                ThreadLocalRandom.current().nextBytes(body);
            } else {
                body = null;
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            ((ExecutorService) client.executor().get()).shutdownNow();
        }

        public HttpClient getClient() {
            return client;
        }

        public byte[] getBody() {
            return body;
        }
    }

    @State(Scope.Thread)
    public static class ThreadState {

        @Param(value = {"64"})
        private int parallelism;
        private List<CompletableFuture<HttpResponse<byte[]>>> futures;

        @Setup
        public void setup() {
            futures = Stream.<CompletableFuture<HttpResponse<byte[]>>>generate(() -> null)
                    .limit(parallelism)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        public List<CompletableFuture<HttpResponse<byte[]>>> getFutures() {
            return futures;
        }
    }


    @Benchmark
    public HttpResponse<byte[]> benchmark(final BenchmarkState benchmarkState, final ThreadState threadState) throws Exception {
        final var client = benchmarkState.getClient();
        final var futures = threadState.getFutures();

        while (true) {
            for (int i = 0; i < futures.size(); ++i) {
                final var future = futures.get(i);
                if (future != null && !future.isDone()) continue;

                var builder = HttpRequest.newBuilder(URI.create("http://localhost:8080/do_request"));
                final var body = benchmarkState.getBody();
                builder = (body != null)
                        ? builder.POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        : builder.GET();

                futures.set(i, client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray()));

                if (future != null) return future.get();
            }
        }
    }
}
