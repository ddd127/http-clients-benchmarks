package com.example.client;

import java.util.concurrent.Future;

import com.example.client.model.ClientRequest;
import com.example.client.model.ClientResponse;

public interface ClientAdapter<REQUEST_TYPE, RESPONSE_TYPE> {

    REQUEST_TYPE mapRequest(final ClientRequest clientRequest);

    @SuppressWarnings("unchecked")
    default ClientResponse mapResponseUnchecked(final Object response) {
        return mapResponse((RESPONSE_TYPE) response);
    }

    ClientResponse mapResponse(final RESPONSE_TYPE response);

    @SuppressWarnings("unchecked")
    default Future<?> sendUnchecked(final Object request) {
        return send((REQUEST_TYPE) request);
    }

    Future<RESPONSE_TYPE> send(final REQUEST_TYPE request);

    void shutdown() throws Exception;
}
