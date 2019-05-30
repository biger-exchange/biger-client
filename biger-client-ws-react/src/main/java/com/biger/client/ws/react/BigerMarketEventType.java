package com.biger.client.ws.react;

import com.biger.client.ws.react.domain.BigerDealEvent;
import com.biger.client.ws.react.domain.BigerMarketDepthEvent;
import com.biger.client.ws.react.domain.BigerSymbolPriceEvent;
import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.domain.response.SymbolEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;


public interface BigerMarketEventType<T> {

    String method();
    TypeReference<T> type();
    String subOp();
    String unsubOp();
    String name();

    static <T> BigerMarketEventType<T> of(String name, String method, String subOp, String unsubOp, TypeReference<T> typeRef) {
        return new BigerMarketEventType<T>() {
            @Override
            public String method() {
                return method;
            }

            @Override
            public TypeReference<T> type() {
                return typeRef;
            }

            @Override
            public String subOp() {
                return subOp;
            }

            @Override
            public String unsubOp() {
                return unsubOp;
            }

            @Override
            public String name() {
                return name;
            }
        };
    }

    @Getter
    @AllArgsConstructor
    @Accessors(fluent = true)
    enum KnownTypes implements BigerMarketEventType{
        PRICE_UPDATE("price.update", new TypeReference<ExchangeResponse<BigerSymbolPriceEvent>>() {
        }, "price.subscribe", "price.unsubscribe"),

        DEPTH_UPDATE("depth.update", new TypeReference<ExchangeResponse<BigerMarketDepthEvent>>() {
        }, "depth.subscribe", "depth.unsubscribe"),

        DEAL_UPDATE("deals.update", new TypeReference<ExchangeResponse<BigerDealEvent>>() {
        }, "deals.subscribe", "deals.unsubscribe"),

        OTHER("unkown", new TypeReference<ExchangeResponse<JsonNode>>() {
        }, "", "");

        public final String method;
        public final TypeReference type;
        public final String subOp;
        public final String unsubOp;
    }

    default String extractKey(SymbolEvent args) {
        return name() + "_" + args.getSymbol();
    }

    default String toKey(String arg) {
        return name() + "_" + arg;
    }


}
