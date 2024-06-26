package com.example.client.impl;

import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;

public class AsyncClientAdapter implements ClientAdapter<Request, Response> {

    private final AsyncHttpClient client;

    public AsyncClientAdapter(final ClientConfiguration configuration) {
        client = asyncHttpClient(
                config()
                        .setIoThreadsCount(configuration.ioThreads())
                        .setMaxConnections(Integer.MAX_VALUE)
                        .setMaxConnectionsPerHost(Integer.MAX_VALUE)
        );
    }

    @Override
    public Request mapRequest(ClientRequest clientRequest) {
        final var builder = new RequestBuilder(clientRequest.getMethod())
                .setUrl(clientRequest.getUrl());
        clientRequest.getBody().ifPresent(builder::setBody);
        return builder.build();
    }

    @Override
    public ClientResponse mapResponse(Response response) {
        return new ClientResponse(response.getStatusCode(), response.getResponseBodyAsBytes());
    }

    @Override
    public ListenableFuture<Response> send(Request request) {
        return client.executeRequest(request);
    }

    @Override
    public void shutdown() throws Exception {
        client.close();
    }
}
