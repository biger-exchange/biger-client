package biger.client.ws.react;

import biger.client.ws.react.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BigerMarketDataWebsocketClient {

    void start();

    Flux<BigerMarketDepthEvent> subMarketDepth(String symbol, int limit, String interval);

    Flux<BigerSymbolPriceEvent> subSymbolPrice(String symbol);

    Mono<String> querySymbolPrice(String symbol);

    Flux<BigerDealEvent> subDeals(String symbol);

    Flux<BigerSymbolStateEvent> subSymbolState(String symbol);

    /**
     * @param type :  PC, H5, APP
     * @param token : token from http api
     * @return
     */
    Mono<LoginAck> login(String type, String token);

    void stop();
}
