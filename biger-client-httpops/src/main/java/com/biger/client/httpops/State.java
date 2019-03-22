package com.biger.client.httpops;

import javax.crypto.Cipher;
import java.time.Clock;

public class State {
    public final HttpOps httpOps;
    public final String url;
    public final String accessToken;
    public final Pool<Cipher> encryptors;
    public final Clock clock;
    public final long expiryLeewayMillis;

    public State(HttpOps httpOps, String url, String accessToken, Pool<Cipher> encryptors, Clock clock, long expiryLeewayMillis) {
        this.httpOps = httpOps;
        this.url = url;
        this.accessToken = accessToken;
        this.encryptors = encryptors;
        this.clock = clock;
        this.expiryLeewayMillis = expiryLeewayMillis;
    }
}
