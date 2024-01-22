package com.example.client.impl;

import java.util.concurrent.Future;

import com.example.client.ClientAdapter;
import com.example.client.ClientConfiguration;
import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;
import org.eclipse.jetty.client.BytesRequestContent;
import org.eclipse.jetty.client.CompletableResponseListener;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class JettyClientAdapter implements ClientAdapter<Request, ContentResponse> {

    private final HttpClient client;

    public JettyClientAdapter(final ClientConfiguration configuration) {
        final var executor = new QueuedThreadPool(configuration.ioThreads());
        this.client = new HttpClient(
                new HttpClientTransportOverHTTP(1)
        );
        client.setExecutor(executor);
        try {
            client.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate Jetty Client", e);
        }
    }

    @Override
    public Request mapRequest(ClientRequest clientRequest) {
        final var request = client.newRequest(clientRequest.getUrl())
                .method(clientRequest.getMethod());
        clientRequest.getBody().ifPresent((body) ->
                request.body(new BytesRequestContent(body)));
        return request;
    }

    @Override
    public ClientResponse mapResponse(ContentResponse response) {
        return new ClientResponse(response.getStatus(), response.getContent());
    }

    @Override
    public Future<ContentResponse> send(Request request) {
        final var listener = new CompletableResponseListener(request);
        return listener.send();
    }

    @Override
    public void shutdown() throws Exception {
        client.stop();
    }
}
