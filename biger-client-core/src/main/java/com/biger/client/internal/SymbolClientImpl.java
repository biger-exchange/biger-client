package com.biger.client.internal;

import com.biger.client.httpops.BigerResponse;
import com.biger.client.SymbolClient;
import com.biger.client.httpops.State;
import com.biger.client.httpops.Utils;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SymbolClientImpl implements SymbolClient {

    final State state;

    public SymbolClientImpl(State state) {
        this.state = state;
    }

    @Override
    public CompletableFuture<List<SymbolInfo>> list() {
        return Utils.<List<SymbolInfo>>req(state,
                "/exchange/api/market/list",
                null,
                "GET",
                null,
                new TypeReference<BigerResponse<List<SymbolInfo>>>() {})
                .thenApply(resp->resp.data());
    }
}
