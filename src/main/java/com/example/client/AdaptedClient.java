package com.example.client;

import com.example.client.impl.JavaClientAdapter;

public enum AdaptedClient {

    JAVA_CLIENT() {

        @Override
        public ClientAdapter<?, ?> createClient(ClientConfiguration configuration) {
            return new JavaClientAdapter(configuration);
        }
    }
    ;

    public abstract ClientAdapter<?, ?> createClient(final ClientConfiguration configuration);

    public static ClientAdapter<?, ?> create(final String clientName,
                                          final ClientConfiguration configuration) {
        return AdaptedClient.valueOf(clientName)
                .createClient(configuration);
    }
}
