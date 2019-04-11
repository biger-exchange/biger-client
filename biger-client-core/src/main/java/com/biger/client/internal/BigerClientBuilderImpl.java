package com.biger.client.internal;

import com.biger.client.BigerClient;
import com.biger.client.OrderClient;
import com.biger.client.SymbolClient;
import com.biger.client.httpops.HttpOps;
import com.biger.client.httpops.HttpOpsBuilder;
import com.biger.client.httpops.Pool;
import com.biger.client.httpops.State;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

public class BigerClientBuilderImpl implements BigerClient.Builder {
    String accessToken;
    String url;
    byte[] privateKey;
    Clock clock = Clock.systemUTC();
    Duration connectionTimeout = Duration.ofSeconds(5);
    Duration expiryLeeway = Duration.ofSeconds(10);

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
    public BigerClient.Builder clock(Clock c) {
        this.clock = Objects.requireNonNull(c);
        return this;
    }

    @Override
    public BigerClient.Builder expiryLeeway(Duration expiryLeeway) {
        this.expiryLeeway = Objects.requireNonNull(expiryLeeway);
        return this;
    }

    @Override
    public BigerClient.Builder connectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = Objects.requireNonNull(connectionTimeout);
        return this;
    }

    @Override
    public BigerClient build() {

        String accessToken = this.accessToken;
        String url = Objects.requireNonNull(this.url);
        Executor executor = this.executor;

        Pool<Cipher> encryptors;

        if (this.privateKey == null) {
            encryptors = new Pool<Cipher>() {
                @Override
                protected Cipher newObject() {
                    try {
                        return Cipher.getInstance("RSA");
                    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } else {
            PrivateKey privateKey;
            try {
                privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(this.privateKey));
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            encryptors = new Pool<Cipher>() {
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
        }

        HttpOps httpOps = HttpOpsBuilder.newBuilder()
                .connectionTimeout(connectionTimeout)
                .executor(executor)
                .build();

        State s = new State(httpOps, url, accessToken, encryptors, clock, expiryLeeway.getNano() / 1000L);
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