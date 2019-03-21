package com.biger.client.httpops;

import javax.crypto.Cipher;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public interface HttpOps {
    CompletableFuture<String> sendAsync(URI uri, String accessToken, Cipher encryptors, String method, String queryString, long expiry, String body, Duration timeout);
}
