package com.biger.client.ws.react;

import com.biger.client.ws.react.domain.*;
import com.biger.client.ws.react.impl.BigerMarketDataWebsocketClientImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public interface BigerMarketDataWebsocketClient extends AutoCloseable {

    Flux<BigerMarketDepthEvent> subMarketDepth(String symbol, int limit, String interval);

    Flux<BigerSymbolPriceEvent> subSymbolPrice(String symbol);

    Mono<String> querySymbolPrice(String symbol);

    Mono<List<KlineDataPoint>> queryKline(String symbol, Instant startTime, Instant endTime, Duration interval);

    Flux<BigerDealEvent> subDeals(String symbol);

    Flux<BigerSymbolStateEvent> subSymbolState(String symbol);

    /**
     * @param type :  PC, H5, APP
     * @param token : token from http api
     * @return
     */
    Mono<LoginAck> login(String type, String token);

    @Override
    void close();

    static Builder newBuilder() {
        return new Builder() {
            AtomicReference<URI> uri = new AtomicReference<>();
            @Override
            public Builder uri(URI uri) {
                this.uri.set(uri);
                return this;
            }

            @Override
            public BigerMarketDataWebsocketClient build() {
                return new BigerMarketDataWebsocketClientImpl(uri.get());
            }
        };
    }

    interface Builder {
        Builder uri(URI uri);

        BigerMarketDataWebsocketClient build();
    }
}
