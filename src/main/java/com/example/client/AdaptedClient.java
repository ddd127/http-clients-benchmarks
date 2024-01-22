package com.example.client;

import com.example.client.impl.ApacheClientAdapter;
import com.example.client.impl.AsyncClientAdapter;
import com.example.client.impl.BaselineClientAdapter;
import com.example.client.impl.JavaClientAdapter;

public enum AdaptedClient {

    BASELINE_CLIENT() {

        @Override
        public ClientAdapter<?, ?> createClient(ClientConfiguration configuration) {
            return new BaselineClientAdapter(configuration);
        }
    },

    JAVA_CLIENT() {

        @Override
        public ClientAdapter<?, ?> createClient(ClientConfiguration configuration) {
            return new JavaClientAdapter(configuration);
        }
    },

    ASYNC_CLIENT() {

        @Override
        public ClientAdapter<?, ?> createClient(ClientConfiguration configuration) {
            return new AsyncClientAdapter(configuration);
        }
    },

    APACHE_CLIENT() {

        @Override
        public ClientAdapter<?, ?> createClient(ClientConfiguration configuration) {
            return new ApacheClientAdapter(configuration);
        }
    },
    ;

    public abstract ClientAdapter<?, ?> createClient(final ClientConfiguration configuration);

    public static ClientAdapter<?, ?> create(final String clientName,
                                          final ClientConfiguration configuration) {
        return AdaptedClient.valueOf(clientName)
                .createClient(configuration);
    }
}
