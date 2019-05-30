package com.biger.client.ws.react.impl;

import com.biger.client.ws.react.BigerMarketDataWebsocketClient;
import com.biger.client.ws.react.BigerMarketEventType;
import com.biger.client.ws.react.BigerMarketRequestType;
import com.biger.client.ws.react.domain.*;
import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.exceptions.BigerExchangeException;
import com.biger.client.ws.react.parser.BigerMarketReponseParser;
import com.biger.client.ws.react.domain.request.*;
import com.biger.client.ws.react.websocket.Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class BigerMarketDataWebsocketClientImpl implements BigerMarketDataWebsocketClient {
    static final Logger LOG = LoggerFactory.getLogger(BigerMarketDataWebsocketClientImpl.class);

    private final Client jsonWebSocketClient;

    final private ObjectMapper objectMapper = BigerMarketReponseParser.INSTANCE.getObjectMapper();

    final private AtomicInteger requestCounter = new AtomicInteger(0);

    final private BigerMarketReponseParser bigerMarketReponseParser = BigerMarketReponseParser.INSTANCE;

    final Function<String, BigerMarketEventType> methodToEventType;

    long generateId() {
        long ans = this.requestCounter.incrementAndGet();
        if (ans > (1 << 20)) {
            this.requestCounter.set(0);
        }
        return ans;
    }

    public BigerMarketDataWebsocketClientImpl(URI address, List<BigerMarketEventType> customEventTypes) {
        Map<String, BigerMarketEventType> m = new HashMap<>();
        for (BigerMarketEventType type : BigerMarketEventType.KnownTypes.values()) {
            m.put(type.method(), type);
        }
        for (BigerMarketEventType type : customEventTypes) {
            m.put(type.method(), type);
        }
        methodToEventType = m::get;

        try {
            jsonWebSocketClient = Client.newBuilder()
                    .uri(address)
                    .text2Response(s-> bigerMarketReponseParser.text2ExchangeResponse(s, methodToEventType))
                    .response2SubId(resp->bigerMarketReponseParser.extractSubIdFromExchangeResponse(resp, methodToEventType))
                    .build()
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Flux<ExchangeResponse> doSub(BigerMarketEventType subType, String key, Object subRequest, Object unSubRequest) {
        String topic = subType.toKey(key);
        return doSub(subType, key, subRequest, unSubRequest, t->t.equals(topic));
    }

    private Flux<ExchangeResponse> doSub(BigerMarketEventType subType, String key, Object subRequest, Object unSubRequest, Predicate<String> filter) {
        try {
            long id = this.generateId();
            String subRequestStr = this.objectMapper.writeValueAsString(ExchangeRequest.builder()
                    .id(id)
                    .method(subType.subOp())
                    .params(subRequest)
                    .build());
            String unSubRequestStr = this.objectMapper.writeValueAsString(ExchangeRequest.builder()
                    .id(this.generateId())
                    .method(subType.unsubOp())
                    .params(unSubRequest)
                    .build());
            LOG.debug("sub request is [{}]", subRequestStr);
            this.jsonWebSocketClient.expectAck(String.valueOf(id)).subscribe(x -> {
                if (x.getError() != null) {
                    LOG.error("Sub for {} failed", subType, x.getError());
                } else {
                    LOG.debug("Sub request for [{}] succeeded, result is [{}]", subType, x);
                }
            }, err -> {
                LOG.warn("Failed to sub for method [{}]", subType, err);
            });
            return this.jsonWebSocketClient.sub(subType.toKey(key), subRequestStr, unSubRequestStr, filter);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialze obj to json", ex);
        }

    }

    private <T> Mono<T> doRequestSingle(BigerMarketRequestType requestType, Object request, Class<T> clazz) {
        long id = this.generateId();
        LOG.debug("request id is [{}]", id);
        ExchangeRequest res = ExchangeRequest.builder()
                .method(requestType.getMethod())
                .params(request)
                .id(id)
                .build();

        String str = null;
        try {
            str = this.objectMapper.writeValueAsString(res);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialze obj to json", ex);
        }

        return this.jsonWebSocketClient.requestSingle(String.valueOf(id), str)
                .map(x -> {
                    if (x.getError() != null) {
                        throw new BigerExchangeException(x.getError().getMessage(), x.getError());
                    }
                    return this.objectMapper.convertValue(x.getResult(), clazz);
                });
    }

    @Override
    public Flux<BigerMarketDepthEvent> subMarketDepth(String symbol, int limit, String interval) {
        return this.doSub(BigerMarketEventType.KnownTypes.DEPTH_UPDATE, symbol, DepthSubRequest.builder().symbol(symbol).limit(limit).interval(interval).build(), DepthUnSubRequest.builder().symbol(symbol).build())
                .map(x -> (BigerMarketDepthEvent) x.getParams());

    }

    @Override
    public Flux<BigerSymbolPriceEvent> subSymbolPrice(String symbol) {
        return this.doSub(BigerMarketEventType.KnownTypes.PRICE_UPDATE, symbol, new SymbolPriceSubRequest(symbol), new SymbolPriceSubRequest(symbol))
                .map(x -> (BigerSymbolPriceEvent) x.getParams());
    }

    @Override
    public Mono<String> querySymbolPrice(String symbol) {
        return this.doRequestSingle(BigerMarketRequestType.PRICER_QUERY, PriceQueryRequest.builder().symbol(symbol).build(), String.class);
    }

    @Override
    public Mono<List<KlineDataPoint>> queryKline(String symbol, Instant startTime, Instant endTime, Duration interval) {
        return this.doRequestSingle(BigerMarketRequestType.KLINE_QUERY, new Object[]{symbol, startTime.getEpochSecond(), endTime.getEpochSecond(), interval.getSeconds()}, JsonNode.class).map(
                arr -> {
                    List<KlineDataPoint> l = new ArrayList<>();
                    if (!arr.isArray()) {
                        return l;
                    }
                    arr.iterator().forEachRemaining(dp-> l.add(new KlineDataPoint(
                            Instant.ofEpochSecond(dp.get(0).asLong()),
                            new BigDecimal(dp.get(1).asText()),
                            new BigDecimal(dp.get(2).asText()),
                            new BigDecimal(dp.get(3).asText()),
                            new BigDecimal(dp.get(4).asText()),
                            new BigDecimal(dp.get(5).asText()),
                            new BigDecimal(dp.get(6).asText())
                    )));
                    return l;
                }
        );
    }

    @Override
    public Flux<BigerDealEvent> subDeals(String symbol) {
        return this.doSub(BigerMarketEventType.KnownTypes.DEAL_UPDATE, symbol, new DealSubRequest(symbol), new DealUnSubRequest(symbol))
                .map(x -> (BigerDealEvent) x.getParams());
    }

    @Override
    public Mono<LoginAck> login(String type, String token) {
        return this.doRequestSingle(BigerMarketRequestType.LOGIN, LoginRequest.builder().token(token).type(type).build(), LoginAck.class);
    }

    @Override
    public <T> Flux<T> customSub(BigerMarketEventType eventType, String key, Object subRequest, Object unsubRequest, Predicate<String> filter) {
        return doSub(eventType, key, subRequest, unsubRequest, filter)
                .map(x -> (T) x.getParams());
    }

    @Override
    public void disconnect() {
        jsonWebSocketClient.interruptConnection();
    }

    @Override
    public void close() {
        this.jsonWebSocketClient.stop();
    }
}
