package com.biger.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SymbolClient {
    CompletableFuture<List<SymbolInfo>> list();

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
