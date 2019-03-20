package com.biger.client.internal;

import javax.crypto.Cipher;
import java.net.http.HttpClient;

class State {
    final HttpClient httpClient;
    final String url;
    final String accessToken;
    final Pool<Cipher> encryptors;

    public State(HttpClient httpClient, String url, String accessToken, Pool<Cipher> encryptors) {
        this.httpClient = httpClient;
        this.url = url;
        this.accessToken = accessToken;
        this.encryptors = encryptors;
    }
}
