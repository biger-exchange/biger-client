package com.biger.client.examples;

import com.biger.client.ws.react.BigerMarketDataWebsocketClient;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;


public class MarketData {
    //    static {
//        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
//    }
    public static void main(String[] args) throws Throwable {

        try (BigerMarketDataWebsocketClient client =
                     BigerMarketDataWebsocketClient.newBuilder()
                             .uri(new URI("wss://biger.pro/ws"))
                             .build()
        ) {
            Disposable d = client.subDeals("BTCUSDT")
                    .subscribeOn(Schedulers.single())
                    .subscribe(System.out::println);
            Thread.sleep(10000);
            client.disconnect();
            Thread.sleep(10000);
            d.dispose();
        }
    }

}
