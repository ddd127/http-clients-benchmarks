package com.example.benchmark.java;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@SuppressWarnings("Duplicates")
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 20)
@Measurement(iterations = 6, time = 20)
public class ThreadPoolExecutorBenchmark {

    @State(Scope.Benchmark)
    public static class ExecutorState {


        @Param(value = {
                "2",
                "4",
                "8",
                "12",
        })
        private int executorSize;
        @Param(value = {
                "256",
        })
        private int parallelism;

        private ExecutorService executorService;
        private List<Future<?>> futures;

        @Setup
        public void setUp() {
            executorService = Executors.newFixedThreadPool(executorSize);
            futures = IntStream.range(0, parallelism)
                    .mapToObj(__ -> (Future<?>) null)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        @TearDown
        public void tearDown() {
            executorService.shutdownNow();
            executorService = null;
        }

        public ExecutorService getExecutorService() {
            return executorService;
        }

        public List<Future<?>> getFutures() {
            return futures;
        }
    }

    @Benchmark
    public Future<?> benchmark(final ExecutorState state, final Blackhole blackhole) {
        final var executor = state.getExecutorService();
        final var futures = state.getFutures();

        while (true) {
            for (int i = 0; i < futures.size(); ++i) {
                final var future = futures.get(i);

                if (future != null && !future.isDone()) {
                    continue;
                }

                final Runnable runnable = () -> {
                    try {
                        final int value = ThreadLocalRandom.current().nextInt();
                        Thread.sleep(1, 1_000 * value % 1000);
                        blackhole.consume(value);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                };
                executor.submit(runnable);
                futures.set(i, executor.submit(runnable));

                if (future != null) {
                    return future;
                }
            }
        }
    }
}
