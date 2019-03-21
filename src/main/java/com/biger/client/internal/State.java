package com.biger.client.internal;

import javax.crypto.Cipher;

class State {
    final HttpOps httpOps;
    final String url;
    final String accessToken;
    final Pool<Cipher> encryptors;

    public State(HttpOps httpOps, String url, String accessToken, Pool<Cipher> encryptors) {
        this.httpOps = httpOps;
        this.url = url;
        this.accessToken = accessToken;
        this.encryptors = encryptors;
    }
}
