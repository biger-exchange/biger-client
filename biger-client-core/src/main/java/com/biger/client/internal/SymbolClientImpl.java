package com.biger.client.internal;

import com.biger.client.httpops.BigerResponse;
import com.biger.client.SymbolClient;
import com.biger.client.httpops.State;
import com.biger.client.httpops.Utils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
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

    @Override
    public CompletableFuture<String> kline(String symbol, Duration interval, Instant startTime, Instant endTime) {

        try {
            return Utils.req(state,
                    "/md/kline",
                    "symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8.name()) +
                            "&period=" + interval.getSeconds() +
                            "&start_time=" + startTime.getEpochSecond() +
                            "&end_time=" + endTime.getEpochSecond(),
                    "GET",
                    null,
                    new TypeReference<BigerResponse<String>>() {})
                    .thenApply(resp->resp.data());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
