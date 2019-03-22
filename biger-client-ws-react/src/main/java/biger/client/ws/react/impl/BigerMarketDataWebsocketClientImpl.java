package biger.client.ws.react.impl;

import biger.client.ws.react.BigerMarketDataWebsocketClient;
import biger.client.ws.react.BigerMarketEventType;
import biger.client.ws.react.BigerMarketRequestType;
import biger.client.ws.react.domain.*;
import biger.client.ws.react.domain.request.*;
import biger.client.ws.react.domain.response.ExchangeResponse;
import biger.client.ws.react.exceptions.BigerExchangeException;
import biger.client.ws.react.parser.BigerMarketReponseParser;
import biger.client.ws.react.websocket.BigerWebSocketClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class BigerMarketDataWebsocketClientImpl implements BigerMarketDataWebsocketClient {
    static final Logger LOG = LoggerFactory.getLogger(BigerMarketDataWebsocketClientImpl.class);

    private final BigerWebSocketClient<ExchangeResponse> jsonWebSocketClient;

    final private ObjectMapper objectMapper = BigerMarketReponseParser.INSTANCE.getObjectMapper();

    final private AtomicInteger requestCounter = new AtomicInteger(0);

    final private Duration pingInterval = Duration.ofSeconds(15);

    final private BigerMarketReponseParser bigerMarketReponseParser = BigerMarketReponseParser.INSTANCE;

    long generateId() {
        long ans = this.requestCounter.incrementAndGet();
        if (ans > (1 << 20)) {
            this.requestCounter.set(0);
        }
        return ans;

    }

    public BigerMarketDataWebsocketClientImpl(String address) {
        jsonWebSocketClient = new BigerWebSocketClient<>(address, bigerMarketReponseParser::text2ExchangeResponse, bigerMarketReponseParser::extractSubIdFromExchangeResponse);
    }

    @Override
    public void start() {

        this.jsonWebSocketClient.start()
                .subscribe(x -> {
                    if (x != 1) {
                        return;
                    }
                    LOG.debug("json websocket client started ", x);

                    this.schedulerForPing();

                    this.jsonWebSocketClient.wildMessages().subscribe(t -> {
                        LOG.warn("Got unkown message [{}]", t);
                    });
                    this.jsonWebSocketClient.unparsedMessages().subscribe(t -> {
                        LOG.warn("Got unparsed message [{}]", t);
                    });
                });
    }

    private void schedulerForPing() {
        this.jsonWebSocketClient.getEventLoopGroup().scheduleWithFixedDelay(() -> {
            this.jsonWebSocketClient.ping();
        }, 10, this.pingInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        this.jsonWebSocketClient.stop().subscribe(x -> {
            if (x == 1) {
                LOG.debug("stopped");
            }
        });
    }

    private Flux<ExchangeResponse> doSub(BigerMarketEventType subType, String key, Object subRequest, Object unSubRequest) {
        try {
            long id = this.generateId();
            String subRequestStr = this.objectMapper.writeValueAsString(ExchangeRequest.builder()
                    .id(id)
                    .method(subType.getSubOp())
                    .params(subRequest)
                    .build());
            String unSubRequestStr = this.objectMapper.writeValueAsString(ExchangeRequest.builder()
                    .id(this.generateId())
                    .method(subType.getUnSubOp())
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
            return this.jsonWebSocketClient.sub(subType.toKey(key), subRequestStr, unSubRequestStr);
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
        return this.doSub(BigerMarketEventType.DEPTH_UPDATE, symbol, DepthSubRequest.builder().symbol(symbol).limit(limit).interval(interval).build(), DepthUnSubRequest.builder().symbol(symbol).build())
                .map(x -> (BigerMarketDepthEvent) x.getParams())
                .share();

    }

    @Override
    public Flux<BigerSymbolPriceEvent> subSymbolPrice(String symbol) {
        return this.doSub(BigerMarketEventType.PRICE_UPDATE, symbol, new SymbolPriceSubRequest(symbol), new SymbolPriceSubRequest(symbol))
                .map(x -> (BigerSymbolPriceEvent) x.getParams())
                .share();
    }

    @Override
    public Mono<String> querySymbolPrice(String symbol) {
        return this.doRequestSingle(BigerMarketRequestType.PRICER_QUERY, PriceQueryRequest.builder().symbol(symbol).build(), String.class);
    }

    @Override
    public Flux<BigerDealEvent> subDeals(String symbol) {
        return this.doSub(BigerMarketEventType.DEAL_UPDATE, symbol, new DealSubRequest(symbol), new DealUnSubRequest(symbol))
                .map(x -> (BigerDealEvent) x.getParams())
                .share();
    }

    @Override
    public Mono<LoginAck> login(String type, String token) {
        return this.doRequestSingle(BigerMarketRequestType.LOGIN, LoginRequest.builder().token(token).type(type).build(), LoginAck.class);
    }

    public Flux<BigerSymbolStateEvent> subSymbolState(String symbol) {
        return this.doSub(BigerMarketEventType.STATE_UPDATE, symbol, new StateSubRequest(symbol), new StateSubRequest(symbol))
                .map(x -> (BigerSymbolStateEvent) x.getParams())
                .share();
    }

}
