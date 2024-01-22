package com.example.client.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;

public class BaselineClientAdapter implements ClientAdapter<HttpRequest, HttpResponse<byte[]>> {

    private final HttpClient client;
    private final ExecutorService executor;

    public BaselineClientAdapter(final ClientConfiguration configuration) {
        this.executor = Executors.newFixedThreadPool(configuration.ioThreads());
        this.client = HttpClient.newBuilder()
                .executor(executor)
                .build();
    }

    @Override
    public HttpRequest mapRequest(final ClientRequest clientRequest) {
        final HttpRequest.BodyPublisher publisher = clientRequest.getBody()
                .map(HttpRequest.BodyPublishers::ofByteArray)
                .orElseGet(HttpRequest.BodyPublishers::noBody);

        return HttpRequest.newBuilder()
                .uri(URI.create(clientRequest.getUrl()))
                .method(clientRequest.getMethod(), publisher)
                .build();
    }

    @Override
    public ClientResponse mapResponse(final HttpResponse<byte[]> response) {
        return new ClientResponse(response.statusCode(), response.body());
    }

    @Override
    public Future<HttpResponse<byte[]>> send(final HttpRequest request) {
        final CompletableFuture<HttpResponse<byte[]>> result = new CompletableFuture<>();
        try {
            final var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            result.complete(response);
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}
