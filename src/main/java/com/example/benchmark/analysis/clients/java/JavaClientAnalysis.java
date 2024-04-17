package com.example.benchmark.analysis.clients.java;

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
@Warmup(iterations = 2, time = 30)
@Measurement(iterations = 4, time = 30)
public class JavaClientAnalysis {

    private static final String URL = Utils.SERVER_URL;

    @State(Scope.Benchmark)
    public static class ClientState {

        @Param(value = {
                "JAVA_CLIENT",
        })
        private String clientName;
        @Param(value = {
//                "2",
                "4",
//                "6",
                "8",
//                "10",
//                "12",
//                "16",
        })
        private int ioThreads;
        @Param(value = {
                "0",
        })
        private int bodySize;

        private JavaClientAdapter client;
        private byte[] body;

        @Setup(Level.Trial)
        public void setup() {
            if (!AdaptedClient.JAVA_CLIENT.name().equals(clientName)) {
                throw new IllegalArgumentException("Wrong client name + '" + clientName + "'");
            }
            client = new JavaClientAdapter(new ClientConfiguration(ioThreads));
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


//    // 2 threads-producers
//
//    public static class ThreadState_Producer_2 extends CommonThreadState {
//        @Override
//        public int getProducerThreads() {
//            return 2;
//        }
//    }
//
//    @Benchmark
//    @Threads(2)
//    public ClientResponse benchmark_producer_2(final ClientState clientState,
//                                               final ThreadState_Producer_2 threadState) throws Exception {
//        return iteration(clientState, threadState);
//    }
//
//
//    // 3 threads-producers
//
//    public static class ThreadState_Producer_3 extends CommonThreadState {
//        @Override
//        public int getProducerThreads() {
//            return 3;
//        }
//    }
//
//    @Benchmark
//    @Threads(3)
//    public ClientResponse benchmark_producer_3(final ClientState clientState,
//                                               final ThreadState_Producer_3 threadState) throws Exception {
//        return iteration(clientState, threadState);
//    }
//
//
//    // 4 threads-producers
//
//    public static class ThreadState_Producer_4 extends CommonThreadState {
//        @Override
//        public int getProducerThreads() {
//            return 4;
//        }
//    }
//
//    @Benchmark
//    @Threads(4)
//    public ClientResponse benchmark_producer_4(final ClientState clientState,
//                                               final ThreadState_Producer_4 threadState) throws Exception {
//        return iteration(clientState, threadState);
//    }
}
