package com.example.client.impl;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;

public class BaselineClientAdapter implements ClientAdapter<ClassicHttpRequest, ClientResponse> {

    private final CloseableHttpClient client;
    private final ExecutorService executor;

    public BaselineClientAdapter(final ClientConfiguration configuration) {
        this.executor = Executors.newFixedThreadPool(configuration.ioThreads());
        this.client = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setMaxConnTotal(Integer.MAX_VALUE)
                                .setMaxConnPerRoute(Integer.MAX_VALUE)
                                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.LAX)
                                .build()
                )
                .build();
    }

    @Override
    public ClassicHttpRequest mapRequest(final ClientRequest clientRequest) {
        final ClassicHttpRequest request = switch (clientRequest.getMethod()) {
            case "GET" -> new HttpGet(clientRequest.getUrl());
            case "POST" -> new HttpPost(clientRequest.getUrl());
            default -> throw new IllegalArgumentException("Unsupported request method " + clientRequest.getMethod());
        };

        clientRequest.getBody().ifPresent((body) ->
                request.setEntity(new ByteArrayEntity(body, ContentType.APPLICATION_OCTET_STREAM))
        );

        return request;
    }

    @Override
    public ClientResponse mapResponse(final ClientResponse response) {
        return response;
    }

    @Override
    public Future<ClientResponse> send(final ClassicHttpRequest request) {
        return executor.submit(() -> client.execute(request, (response) ->
                new ClientResponse(response.getCode(), response.getEntity().getContent().readAllBytes())
        ));
    }

    @Override
    public void shutdown() throws Exception {
        executor.shutdownNow();
        client.close();
    }
}
