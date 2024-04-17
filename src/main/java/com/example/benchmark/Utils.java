package com.example.benchmark;

public class Utils {

    private Utils() {
        throw new IllegalStateException();
    }

    public static final String SERVER_URL;

    static {
        final var url = System.getProperty("nginx_url", "localhost");
        SERVER_URL = "http://" + url + "/do_request";
    }
}
