package com.biger.client.ws.react;

import com.biger.client.ws.react.domain.BigerDealEvent;
import com.biger.client.ws.react.domain.BigerMarketDepthEvent;
import com.biger.client.ws.react.domain.BigerSymbolPriceEvent;
import com.biger.client.ws.react.domain.BigerSymbolStateEvent;
import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.domain.response.SymbolEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum BigerMarketEventType {

    PRICE_UPDATE("price.update", new TypeReference<ExchangeResponse<BigerSymbolPriceEvent>>() {
    }, "price.subscribe", "price.unsubscribe"),

    DEPTH_UPDATE("depth.update", new TypeReference<ExchangeResponse<BigerMarketDepthEvent>>() {
    }, "depth.subscribe", "depth.unsubscribe"),

    DEAL_UPDATE("deals.update", new TypeReference<ExchangeResponse<BigerDealEvent>>() {
    }, "deals.subscribe", "deals.unsubscribe"),

    STATE_UPDATE("state.update", new TypeReference<ExchangeResponse<BigerSymbolStateEvent>>() {
    }, "state.subscribe", "state.unsubscribe"),

    OTHER("unkown", null, "", "");

    private String method;

    private TypeReference type;

    private String subOp;

    private String unSubOp;

    static public BigerMarketEventType from(String s) {
        return Arrays.stream(BigerMarketEventType.values())
                .filter(x -> x != OTHER)
                .filter(x -> x.method.equalsIgnoreCase(s))
                .findAny()
                .orElse(OTHER);
    }

    public String extractKey(SymbolEvent args) {
        return this.name() + "_" + args.getSymbol();
    }

    public String toKey(String arg) {
        return this.name() + "_" + arg;
    }


}
