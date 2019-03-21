package com.biger.client.httpops;

import javax.crypto.Cipher;

public class State {
    public final HttpOps httpOps;
    public final String url;
    public final String accessToken;
    public final Pool<Cipher> encryptors;

    public State(HttpOps httpOps, String url, String accessToken, Pool<Cipher> encryptors) {
        this.httpOps = httpOps;
        this.url = url;
        this.accessToken = accessToken;
        this.encryptors = encryptors;
    }
}
