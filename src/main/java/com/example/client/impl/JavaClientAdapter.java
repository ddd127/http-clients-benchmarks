package com.example.client.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;

public class JavaClientAdapter implements ClientAdapter<HttpRequest, HttpResponse<byte[]>> {

    private final ExecutorService executor;
    private final HttpClient client;

    public JavaClientAdapter(final ClientConfiguration configuration) {
        this(Executors.newFixedThreadPool(configuration.ioThreads()));
    }

    public JavaClientAdapter(final ClientConfiguration configuration, final BlockingQueue<Runnable> queue) {
        this(new ThreadPoolExecutor(
                configuration.ioThreads(),
                configuration.ioThreads(),
                0L,
                TimeUnit.MILLISECONDS,
                queue
        ));
    }

    public JavaClientAdapter(final ExecutorService executor) {
        this.executor = executor;
        this.client = HttpClient.newBuilder()
                .executor(executor)
                .build();
    }

    @Override
    public HttpRequest mapRequest(ClientRequest clientRequest) {
        final HttpRequest.BodyPublisher publisher = clientRequest.getBody()
                .map(HttpRequest.BodyPublishers::ofByteArray)
                .orElseGet(HttpRequest.BodyPublishers::noBody);

        return HttpRequest.newBuilder()
                .uri(URI.create(clientRequest.getUrl()))
                .method(clientRequest.getMethod(), publisher)
                .build();
    }

    @Override
    public ClientResponse mapResponse(HttpResponse<byte[]> response) {
        return new ClientResponse(response.statusCode(), response.body());
    }

    @Override
    public Future<HttpResponse<byte[]>> send(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}
