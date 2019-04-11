package com.biger.client;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

public interface SymbolClient {
    CompletableFuture<List<SymbolInfo>> list();

    /**
     * beta api - subject to changes
     */
    CompletableFuture<List<KlineDataPoint>> kline(String symbol, Duration interval, Instant startTime, Instant endTime);

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

    class KlineDataPoint {
        public final Instant time;
        public final BigDecimal openPrice;
        public final BigDecimal lastPrice;
        public final BigDecimal highPrice;
        public final BigDecimal lowPrice;
        public final BigDecimal volume;
        public final BigDecimal tradeValue;

        public KlineDataPoint(Instant time, BigDecimal openPrice, BigDecimal lastPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal volume, BigDecimal tradeValue) {
            this.time = time;
            this.openPrice = openPrice;
            this.lastPrice = lastPrice;
            this.highPrice = highPrice;
            this.lowPrice = lowPrice;
            this.volume = volume;
            this.tradeValue = tradeValue;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", KlineDataPoint.class.getSimpleName() + "[", "]")
                    .add("time=" + time)
                    .add("openPrice=" + openPrice)
                    .add("lastPrice=" + lastPrice)
                    .add("highPrice=" + highPrice)
                    .add("lowPrice=" + lowPrice)
                    .add("volume=" + volume)
                    .add("tradeValue=" + tradeValue)
                    .toString();
        }
    }
}
