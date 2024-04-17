package com.example.client.model;

import java.util.Optional;

public class ClientRequest {

    private final String url;

    private final byte[] body;

    public ClientRequest(final String url,
                         final byte[] body) {
        this.url = url;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return body != null ? "POST" : "GET";
    }

    public Optional<byte[]> getBody() {
        return Optional.ofNullable(body);
    }
}
