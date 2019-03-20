package com.biger.client;

import com.biger.client.internal.BigerClientBuilderImpl;

import java.util.concurrent.Executor;

public interface BigerClient {
    OrderClient orders();
    SymbolClient symbols();

    static Builder builder() {
        return new BigerClientBuilderImpl();
    }

    interface Builder {
        Builder accessToken(String accessToken);
        Builder privateKey(byte[] privateKey);
        Builder url(String url);
        Builder executor(Executor executor);
        BigerClient build();
    }

}
