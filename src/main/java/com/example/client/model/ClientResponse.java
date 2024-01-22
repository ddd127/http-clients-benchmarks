package com.example.client.model;

public class ClientResponse {

    private final int status;
    private final byte[] content;

    public ClientResponse(final int status, final byte[] content) {
        this.status = status;
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public byte[] getContent() {
        return content;
    }
}
