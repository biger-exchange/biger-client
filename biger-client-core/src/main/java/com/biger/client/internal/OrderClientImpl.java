package com.biger.client.internal;

import com.biger.client.httpops.BigerResponse;
import com.biger.client.OrderClient;
import com.biger.client.httpops.State;
import com.biger.client.httpops.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


public class OrderClientImpl implements OrderClient {
    final State state;

    public OrderClientImpl(State state) {
        this.state = state;
    }

    @Override
    public CompletableFuture<OrderInfo> query(String orderId) {
        return Utils.req(
                state,
                "/exchange/orders/get/orderId/" + Utils.encodePath(orderId),
                null,
                "GET",
                null,
                new TypeReference<BigerResponse<OrderInfo>>() {
                }
        ).thenApply(BigerResponse::data);
    }

    @Override
    public CompletableFuture<OrderInfo> createLimitOrder(String symbol, boolean isBuy, BigDecimal unitPrice, BigDecimal qty) {
        String body = null;
        try {
            Map<String, String> m = new HashMap<>();
            m.put("symbol", symbol);
            m.put("side", isBuy? "BUY": "SELL");
            m.put("price", unitPrice.toPlainString());
            m.put("orderQty", qty.toPlainString());
            m.put("orderType", "LIMIT");
            body = Utils.m.writeValueAsString(m);
//            body = Utils.m.writeValueAsString(Map.of(
//                    "symbol", symbol,
//                    "side", isBuy? "BUY": "SELL",
//                    "price", unitPrice.toPlainString(),
//                    "orderQty", qty.toPlainString(),
//                    "orderType", "LIMIT"
//            ));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error serializing request body", e);
        }
        return Utils.req(
                state,
                "/exchange/orders/create",
                null,
                "POST",
                body,
                new TypeReference<BigerResponse<OrderInfo>>() {
                }
        ).thenApply(BigerResponse::data);
    }

    @Override
    public CompletableFuture<Void> cancel(String orderId) {
        return Utils.req(
                state,
                "/exchange/orders/cancel/" + Utils.encodePath(orderId),
                null,
                "PUT",
                null,
                new TypeReference<BigerResponse<Void>>() {
                }
        ).thenApply(resp->null);
    }
}
