package com.biger.client.ws.react.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.StringJoiner;

public class KlineDataPoint {
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

    public String toString() {
        return (new StringJoiner(", ", KlineDataPoint.class.getSimpleName() + "[", "]")).add("time=" + this.time).add("openPrice=" + this.openPrice).add("lastPrice=" + this.lastPrice).add("highPrice=" + this.highPrice).add("lowPrice=" + this.lowPrice).add("volume=" + this.volume).add("tradeValue=" + this.tradeValue).toString();
    }
}
