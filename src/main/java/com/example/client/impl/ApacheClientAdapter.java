package com.example.client.impl;

import java.util.concurrent.Future;

import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;

public class ApacheClientAdapter implements ClientAdapter<SimpleHttpRequest, SimpleHttpResponse> {

    private final CloseableHttpAsyncClient client;

    public ApacheClientAdapter(final ClientConfiguration configuration) {
        this(configuration, PoolConcurrencyPolicy.LAX);
    }

    public ApacheClientAdapter(final ClientConfiguration configuration, final PoolConcurrencyPolicy pollPolicy) {
        this.client = HttpAsyncClients.custom()
                .setIOReactorConfig(
                        IOReactorConfig.custom()
                                .setIoThreadCount(configuration.ioThreads())
                                .build()
                )
                .setConnectionManager(
                        PoolingAsyncClientConnectionManagerBuilder.create()
                                .setPoolConcurrencyPolicy(pollPolicy)
                                .setMaxConnTotal(Integer.MAX_VALUE)
                                .setMaxConnPerRoute(Integer.MAX_VALUE)
                                .build()
                )
                .build();
        client.start();
    }

    @Override
    public SimpleHttpRequest mapRequest(ClientRequest clientRequest) {
        final var request = SimpleHttpRequest.create(clientRequest.getMethod(), clientRequest.getUrl());
        clientRequest.getBody().ifPresent((body) -> request.setBody(body, ContentType.APPLICATION_OCTET_STREAM));
        return request;
    }

    @Override
    public ClientResponse mapResponse(SimpleHttpResponse response) {
        return new ClientResponse(response.getCode(), response.getBodyBytes());
    }

    @Override
    public Future<SimpleHttpResponse> send(SimpleHttpRequest request) {
        return client.execute(request, null);
    }

    @Override
    public void shutdown() throws Exception {
        client.close();
    }
}
