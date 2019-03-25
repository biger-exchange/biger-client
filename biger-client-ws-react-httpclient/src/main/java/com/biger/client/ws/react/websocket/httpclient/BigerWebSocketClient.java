package com.biger.client.ws.react.websocket.httpclient;


import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.websocket.Client;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings({"unsafe", "unchecked"})
public class BigerWebSocketClient implements Client {
    static final Logger LOG = LoggerFactory.getLogger(BigerWebSocketClient.class);

    static final int DEFAULT_MAX_FRAME_SIZE = 8192;

    static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    static final Duration DEFAULT_RETRY_TIMEOUT = Duration.ofSeconds(2);

    @Getter
    private final int maxFrameSize;

    @Getter
    private final Duration connectTimeout;

    private boolean connectedSuccessfully = false;

    private final Function<String, ExchangeResponse> text2MessageConvertorFunc;

    private final Function<ExchangeResponse, String> msgIdExtractorFunc;

    @Getter
    private final Duration retryTimeout;

    // for push update
    private final ConcurrentMap<String, Subscription> subIdMap = new ConcurrentHashMap<>();

    private final Map<String, Flux<ExchangeResponse>> topic2FluxMap = new HashMap<>();

    // for requestSingle
    private final ConcurrentMap<String, MonoSink> ackSinkMap = new ConcurrentHashMap<>();

    private AtomicBoolean started = new AtomicBoolean(false);

    private boolean isManualDisconnect = false;

    @Getter
    final private URI wsUri;

    private FluxSink<ExchangeResponse> unkonwMessageEmitter;

    private FluxSink<String> unparsedMsgEmitter;

    private final Flux<String> unparsedMsgFlux;

    private final Flux<ExchangeResponse> unkonwMessageFlux;

    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    final Timer t = new Timer();

    @Setter
    private boolean acceptAllCertificates;

    volatile WebSocket ws;

    public BigerWebSocketClient(URI address, Function<String, ExchangeResponse> text2MsgFunc, Function<ExchangeResponse, String> msg2SubIdFunc) {
        this(address, text2MsgFunc, msg2SubIdFunc, DEFAULT_MAX_FRAME_SIZE, DEFAULT_CONNECT_TIMEOUT, DEFAULT_RETRY_TIMEOUT);
    }

    public BigerWebSocketClient(URI address, Function<String, ExchangeResponse> text2MsgFunc, Function<ExchangeResponse, String> msg2SubIdFunc, int maxFramePayload, Duration connectionTimeout, Duration retryTimeout) {
        this.wsUri = address;

        this.text2MessageConvertorFunc = text2MsgFunc;
        this.msgIdExtractorFunc = msg2SubIdFunc;
        this.maxFrameSize = maxFramePayload;
        this.connectTimeout = connectionTimeout;
        this.retryTimeout = retryTimeout;

        this.unkonwMessageFlux = Flux.create(emitter -> {
            this.unkonwMessageEmitter = emitter;
        }, FluxSink.OverflowStrategy.LATEST);
        this.unkonwMessageFlux.subscribe();

        this.unparsedMsgFlux = Flux.<String>create(sink -> {
            this.unparsedMsgEmitter = sink;
        }, FluxSink.OverflowStrategy.LATEST);
        this.unparsedMsgFlux.subscribe();
    }

    @Override
    public void schedulePing(Duration pingInterval) {
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    ping();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0L, pingInterval.toNanos() / 1000L);
    }

    static class Subscription {
        final FluxSink fluxSink;
        final String subId;
        final String subRequest;
        final String unSubRequest;

        public Subscription(FluxSink flux, String subId, String subRequest, String unSubRequest) {
            this.fluxSink = flux;
            this.subId = subId;
            this.subRequest = subRequest;
            this.unSubRequest = unSubRequest;
        }
    }

    @Override
    public Mono<Integer> start() {
        if (!this.started.compareAndSet(false, true)) {
            return Mono.just(0);
        }
        return doStart();
    }

    public Mono<Integer> doStart() {

        HttpClient c = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Handler h = new Handler(msg->onMessageReceived(msg));

        CompletableFuture<WebSocket> f = c.newWebSocketBuilder()
                .buildAsync(wsUri, h);

        return Mono.create(sink-> f.whenComplete((webSocket, throwable) -> {
            if (throwable == null) {
                ws = webSocket;
                sink.success(1);
                return;
            }
            sink.error(throwable);
        }));

    }

    static private int extractPort(URI uri) {
        int port = uri.getPort();
        if (port != -1) {
            return port;
        }

        String scheme = uri.getScheme();
        if ("ws".equalsIgnoreCase(scheme)) {
            return 80;
        }
        if ("wss".equalsIgnoreCase(scheme)) {
            return 443;
        }

        throw new IllegalStateException("Only websocket scheme supported, uri started with ws(s)://");
    }

    @Override
    public Flux<ExchangeResponse> wildMessages() {
        return this.unkonwMessageFlux.share();
    }

    @Override
    public Flux<String> unparsedMessages() {
        return this.unparsedMsgFlux.share();
    }

    private void onMessageReceived(CharSequence cs) {
        String text = cs.toString();
        LOG.debug("Websocket message received [{}]", text);
        ExchangeResponse msg = Optional.ofNullable(text).map(this.text2MessageConvertorFunc).orElse(null);
        if (msg == null) {
            LOG.warn("Failed to parse message [{}]", text);
            this.unparsedMsgEmitter.next(text);
            return;
        }

        String subId = Optional.ofNullable(msg).map(this.msgIdExtractorFunc).orElse(null);
        if (subId == null) {
            LOG.warn("Failed to extract sub id from message");
            this.unkonwMessageEmitter.next(msg);
            return;
        }
        this.dispatchMessage(subId, msg);
    }

    private void dispatchMessage(String subId, ExchangeResponse message) {
        assert subId != null : "subId should not null in dispatch";
        MonoSink ackEmiiter = this.ackSinkMap.get(subId);
        if (ackEmiiter != null) {
            ackEmiiter.success(message);
            return;
        }

        if (!Optional.ofNullable(subIdMap.get(subId))
                .filter(Objects::nonNull)
                .map(x -> x.fluxSink.next(message)).isPresent()) {
            LOG.warn("Failed to find receivers for sub id [{}]", subId);
            this.unkonwMessageEmitter.next(message);
        }
    }

    private void ensureSocketReady() throws IOException {
        if (this.ws == null || ws.isInputClosed()|| ws.isOutputClosed()) {
            LOG.warn("WebSocket is not open! Call connect first.");
            throw new IOException("Websocket is not connected yet");
        }
    }

    public void sendMessage(String message) throws IOException {
        LOG.debug("Sending message: {}", message);
        this.ensureSocketReady();

        if (message != null) {
            ws.sendText(message, true).join();
        }
    }

    @Override
    public Mono<ExchangeResponse> expectAck(String requsetId) {
        return Mono.create(emitter -> {
            MonoSink prevSink = this.ackSinkMap.putIfAbsent(requsetId, emitter);
            if (prevSink != null) {
                emitter.error(new IllegalStateException("duplicate requestId " + requsetId));
                return;
            }
            LOG.debug("request [{}] expecting", requsetId);
            emitter.onDispose(() -> {
                if (prevSink == null) {
                    this.ackSinkMap.remove(requsetId, emitter);
                }
            });
        });
    }

    @Override
    public Mono<ExchangeResponse> requestSingle(String requestId, String requestMsg) {
        return Mono.<ExchangeResponse>create(monoSink -> {
            MonoSink prevSink = this.ackSinkMap.putIfAbsent(requestId, monoSink);
            if (prevSink != null) {
                monoSink.error(new IllegalStateException("duplicate request id in request single " + requestId));
                return;
            }
            try {
                this.sendMessage(requestMsg);
            } catch (IOException e) {
                LOG.warn("Failed to send message, give up ", e);
                monoSink.error(e);
                return;
            }
            monoSink.onDispose(() -> {
                if (prevSink == null) {
                    this.ackSinkMap.remove(requestId, monoSink);
                }

            });
        }).doOnError(err -> {
            LOG.warn("Got error in request single for requsetId " + requestId, err);
        });
    }

    public void addAckMonoSink(String requestId, MonoSink<ExchangeResponse> monoSink) {
        this.ackSinkMap.putIfAbsent(requestId, monoSink);
    }

    public Mono<Integer> stop() {
        if (!this.started.compareAndSet(true,false)) {
            return Mono.just(0);
        }

        isManualDisconnect = true;
        connectedSuccessfully = false;
        return Mono.create(sink -> {
            if (ws != null) {
                ws.abort();
            }
            t.cancel();
        });
    }

    @Override
    public Flux<ExchangeResponse> sub(String subId, String subRequestMsg, String unSubRequestMsg) {
        return this.sub(subId, subRequestMsg, unSubRequestMsg, FluxSink.OverflowStrategy.LATEST);
    }

    public Flux<ExchangeResponse> sub(String subId, String subRequestMsg, String unSubRequestMsg, FluxSink.OverflowStrategy overflowStrategy) {
        LOG.debug("Subscribing to websocket Channel {}", subId);
        Lock lock = locks.computeIfAbsent(subId, k-> new ReentrantLock());
        lock.lock();
        try {
            Flux<ExchangeResponse> sharedFlux = this.topic2FluxMap.get(subId);
            if (sharedFlux != null) {
                return sharedFlux.share();
            }

            Flux<ExchangeResponse> ans = Flux.<ExchangeResponse>create(fluxSink -> {
                Subscription prev = this.subIdMap.putIfAbsent(subId, new Subscription(fluxSink, subId, subRequestMsg, unSubRequestMsg));

                if (prev == null) {
                    try {
                        sendMessage(subRequestMsg);
                    } catch (IOException e) {
                        //ignore
                    }
                }

                fluxSink.onDispose(() -> {
                    Subscription rprev = this.subIdMap.remove(subId);
                    if (rprev != null) {
                        try {
                            sendMessage(unSubRequestMsg);
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                });
            }, overflowStrategy)
                    .retryWhen((Flux<Throwable> errFlux) -> errFlux.filter(err -> !(err instanceof IOException)).flatMap(err -> Mono.delay(this.retryTimeout)))
                    .share();

            this.topic2FluxMap.put(subId, ans);
            return ans;
        } finally {
            lock.unlock();
        }
    }

    public Flux<ExchangeResponse> sub(String subId, BiFunction<String, Object[], String> requestMsgFunc, Object[] args, BiFunction<String, Object[], String> unSubRequestMsgFunc, FluxSink.OverflowStrategy overflowStrategy) {
        return this.sub(subId, requestMsgFunc.apply(subId, args), unSubRequestMsgFunc.apply(subId, args), overflowStrategy);
    }

    private void resub() {
        this.subIdMap.entrySet().forEach(x -> {
            try {
                sendMessage(x.getValue().subRequest);
            } catch (Exception ex) {
                LOG.warn("Failed to resub for [{}]", x.getValue(), ex);
            }
        });
    }


    static ByteBuffer pingPayload = ByteBuffer.wrap(new byte[]{8, 1, 8, 1});

    public void ping() throws IOException {
        LOG.debug("Sending ping message");
        ensureSocketReady();

        ws.sendPing(pingPayload).join();
    }
}
