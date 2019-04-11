package com.biger.client;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SymbolClient {
    CompletableFuture<List<SymbolInfo>> list();
    CompletableFuture<String> kline(String symbol, Duration interval, Instant startTime, Instant endTime);

    class SymbolInfo {
        public String symbol;
        public String baseCurrency;
        public String quoteCurrency;

        @Override
        public String toString() {
            return "SymbolInfo{" +
                    "symbol='" + symbol + '\'' +
                    ", baseCurrency='" + baseCurrency + '\'' +
                    ", quoteCurrency='" + quoteCurrency + '\'' +
                    '}';
        }
    }
}
