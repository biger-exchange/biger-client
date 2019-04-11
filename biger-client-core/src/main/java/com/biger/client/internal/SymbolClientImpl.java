package com.biger.client.internal;

import com.biger.client.httpops.BigerResponse;
import com.biger.client.SymbolClient;
import com.biger.client.httpops.BigerResponseException;
import com.biger.client.httpops.State;
import com.biger.client.httpops.Utils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
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
    public CompletableFuture<List<KlineDataPoint>> kline(String symbol, Duration interval, Instant startTime, Instant endTime) {
        try {
            return Utils.reqGeneric(state,
                    "/md/kline",
                    "symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8.name()) +
                            "&period=" + interval.getSeconds() +
                            "&start_time=" + startTime.getEpochSecond() +
                            "&end_time=" + endTime.getEpochSecond(),
                    "GET",
                    null)
                    .thenApply(resp->{
                        JsonNode error = resp.get("error");
                        if(error != null && !error.isNull()) {
                            throw new BigerResponseException(error.asText());
                        }
                        JsonNode result = resp.get("result");
                        if(result == null || result.isNull()) {
                            throw new BigerResponseException("missing result");
                        }
                        List<KlineDataPoint> l = new ArrayList<>();
                        Iterator<JsonNode> elements = result.elements();
                        while(elements.hasNext()) {
                            JsonNode dp = elements.next();
                            l.add(new KlineDataPoint(
                                    Instant.ofEpochSecond(dp.get(0).asLong()),
                                    new BigDecimal(dp.get(1).asText()),
                                    new BigDecimal(dp.get(2).asText()),
                                    new BigDecimal(dp.get(3).asText()),
                                    new BigDecimal(dp.get(4).asText()),
                                    new BigDecimal(dp.get(5).asText()),
                                    new BigDecimal(dp.get(6).asText())
                            ));
                        }
                        return l;
                    });
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
