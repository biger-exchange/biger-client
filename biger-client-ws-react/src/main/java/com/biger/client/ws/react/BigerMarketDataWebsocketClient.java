package com.biger.client.ws.react;

import com.biger.client.ws.react.domain.*;
import com.biger.client.ws.react.impl.BigerMarketDataWebsocketClientImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public interface BigerMarketDataWebsocketClient extends AutoCloseable {

    Flux<BigerMarketDepthEvent> subMarketDepth(String symbol, int limit, String interval);

    Flux<BigerSymbolPriceEvent> subSymbolPrice(String symbol);

    Mono<String> querySymbolPrice(String symbol);

    Mono<List<KlineDataPoint>> queryKline(String symbol, Instant startTime, Instant endTime, Duration interval);

    Flux<BigerDealEvent> subDeals(String symbol);

    /**
     * @param type :  PC, H5, APP
     * @param token : token from http api
     * @return
     */
    Mono<LoginAck> login(String type, String token);

    <T> Flux<T> customSub(BigerMarketEventType eventType, String key, Object subRequest, Object unsubRequest, Predicate<String> filter);

    /**
     * there prob shouldnt be any good reason to do this other than for testing, do not use this unless you know what you are doing
     */
    void disconnect();

    @Override
    void close();

    static Builder newBuilder() {
        return new Builder() {
            AtomicReference<URI> uri = new AtomicReference<>();
            List<BigerMarketEventType> customEventTypes = new ArrayList<>();
            @Override
            public Builder uri(URI uri) {
                this.uri.set(uri);
                return this;
            }

            @Override
            public Builder addBigerMarketEventType(BigerMarketEventType t) {
                customEventTypes.add(t);
                return this;
            }

            @Override
            public BigerMarketDataWebsocketClient build() {
                return new BigerMarketDataWebsocketClientImpl(uri.get(), customEventTypes);
            }
        };
    }

    interface Builder {
        Builder uri(URI uri);
        Builder addBigerMarketEventType(BigerMarketEventType t);

        BigerMarketDataWebsocketClient build();
    }
}
