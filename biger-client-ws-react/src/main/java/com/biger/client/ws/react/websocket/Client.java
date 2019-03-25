package com.biger.client.ws.react.websocket;

import com.biger.client.ws.react.domain.response.ExchangeResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ServiceLoader;

public interface Client {
    void schedulePing(Duration pingInterval);

    Mono<Integer> start();

    Flux<ExchangeResponse> wildMessages();

    Flux<String> unparsedMessages();

    Mono<ExchangeResponse> expectAck(String requsetId);

    Mono<ExchangeResponse> requestSingle(String requestId, String requestMsg);

    Flux<ExchangeResponse> sub(String subId, String subRequestMsg, String unSubRequestMsg);

    static ClientBuilder newBuilder() {
        return ServiceLoader.load(ClientBuilder.class).iterator().next();
    }

    Mono<Integer> stop();
}
