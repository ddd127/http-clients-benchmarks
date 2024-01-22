package com.example.client.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;

public class JavaClientAdapter implements ClientAdapter<HttpRequest, HttpResponse<byte[]>> {

    private final ExecutorService executor;
    private final HttpClient client;

    public JavaClientAdapter(final ClientConfiguration configuration) {
        this.executor = Executors.newFixedThreadPool(configuration.ioThreads());
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
    public CompletableFuture<HttpResponse<byte[]>> send(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}
