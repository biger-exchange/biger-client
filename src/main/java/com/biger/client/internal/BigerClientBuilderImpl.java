package com.biger.client.internal;

import com.biger.client.BigerClient;
import com.biger.client.OrderClient;
import com.biger.client.SymbolClient;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.net.http.HttpClient;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

public class BigerClientBuilderImpl implements BigerClient.Builder {
    String accessToken;
    String url;
    byte[] privateKey;

    Executor executor;

    @Override
    public BigerClient.Builder accessToken(String accessToken) {
        this.accessToken = Objects.requireNonNull(accessToken);
        return this;
    }

    @Override
    public BigerClient.Builder privateKey(byte[] privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    @Override
    public BigerClient.Builder url(String url) {
        this.url = Objects.requireNonNull(url);
        return this;
    }

    @Override
    public BigerClient.Builder executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public BigerClient build() {

        String accessToken = Objects.requireNonNull(this.accessToken);
        String url = Objects.requireNonNull(this.url);
        Executor executor = this.executor;

        PrivateKey privateKey;
        try {
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(this.privateKey));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Pool<Cipher> encryptors = new Pool<Cipher>() {
            @Override
            protected Cipher newObject() {
                try {
                    return Cipher.getInstance("RSA");
                } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void prepareObject(Cipher cipher) {
                try {
                    cipher.init(Cipher.ENCRYPT_MODE, privateKey, new SecureRandom());
                } catch (InvalidKeyException e) {
                    throw new IllegalStateException(e);
                }
            }
        };

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5));

        if (executor != null) {
            httpClientBuilder = httpClientBuilder.executor(executor);
        }
        HttpClient httpClient = httpClientBuilder.build();

        State s = new State(httpClient, url, accessToken, encryptors);
        SymbolClientImpl symbolClient = new SymbolClientImpl(s);
        OrderClientImpl orderClient = new OrderClientImpl(s);

        return new BigerClient() {
            @Override
            public OrderClient orders() {
                return orderClient;
            }

            @Override
            public SymbolClient symbols() {
                return symbolClient;
            }
        };
    }
}