package com.biger.client.ws.react.websocket.httpclient;


import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.websocket.Client;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

@SuppressWarnings({"unsafe", "unchecked"})
public class BigerWebSocketClient implements Client {
    static final Logger LOG = LoggerFactory.getLogger(BigerWebSocketClient.class);

    static final int DEFAULT_MAX_FRAME_SIZE = 8192;

    static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    static final Duration DEFAULT_RETRY_TIMEOUT = Duration.ofSeconds(2);

    private final Duration connectTimeout;

    private final Function<String, ExchangeResponse> text2MessageConvertorFunc;

    private final Function<ExchangeResponse, String> msgIdExtractorFunc;

    private final Duration retryTimeout;

    // for push update
    private final ConcurrentMap<String, Subscription> subIdMap = new ConcurrentHashMap<>();

    private final Map<String, Flux<ExchangeResponse>> topic2FluxMap = new HashMap<>();

    // for requestSingle
    private final ConcurrentMap<String, MonoSink> ackSinkMap = new ConcurrentHashMap<>();

    final private URI wsUri;

    private final ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();

    final Timer t = new Timer();
    final TimerTask pingTask = new TimerTask() {
        @Override
        public void run() {
            try {
                ping();
            } catch (IOException e) {
                LOG.warn("ping failed", e);

            } catch (NullPointerException e) {}
        }
    };

    volatile WebSocket ws;
    final Lock wsLock = new ReentrantLock();
    volatile boolean stopped = false;

    final Handler handler = new Handler(msg->onMessageReceived(msg), t-> {
        wsLock.lock();
        ws = null;
        wsLock.unlock();
    });

    private BigerWebSocketClient(URI address, Function<String, ExchangeResponse> text2MsgFunc, Function<ExchangeResponse, String> msg2SubIdFunc) {
        this(address, text2MsgFunc, msg2SubIdFunc, DEFAULT_MAX_FRAME_SIZE, DEFAULT_CONNECT_TIMEOUT, DEFAULT_RETRY_TIMEOUT);
    }

    private BigerWebSocketClient(URI address, Function<String, ExchangeResponse> text2MsgFunc, Function<ExchangeResponse, String> msg2SubIdFunc, int maxFramePayload, Duration connectionTimeout, Duration retryTimeout) {
        this.wsUri = address;

        this.text2MessageConvertorFunc = text2MsgFunc;
        this.msgIdExtractorFunc = msg2SubIdFunc;
        this.connectTimeout = connectionTimeout;
        this.retryTimeout = retryTimeout;

    }

    static CompletableFuture<BigerWebSocketClient> build(URI address, Function<String, ExchangeResponse> text2MsgFunc, Function<ExchangeResponse, String> msg2SubIdFunc) {
        BigerWebSocketClient c = new BigerWebSocketClient(address, text2MsgFunc, msg2SubIdFunc);
        c.t.schedule(c.pingTask, 0L, 15000L);
        return CompletableFuture.completedFuture(c);
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

    private WebSocket ws() {
        WebSocket ws = this.ws;
        if (ws != null) return ws;

        if (stopped) throw new IllegalStateException("already stopped");

        wsLock.lock();
        try {
            while (true) { // TODO - notify app of connection issues if this takes too long
                try {
                    this.ws = connect().get();
                    for (Map.Entry<String, Subscription> x : this.subIdMap.entrySet()) {
                        try {
                            sendMessage(x.getValue().subRequest);
                        } catch (Exception ex) {
                            LOG.warn("Failed to resub for [{}]", x.getValue(), ex);
                        }
                    }
                    return this.ws;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    LOG.error("error connecting ws", e);
                }
            }
        } finally {
            wsLock.unlock();
        }
    }

    private CompletableFuture<WebSocket> connect() {

        HttpClient c = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        return c.newWebSocketBuilder()
                .buildAsync(wsUri, handler);
    }

    private void onMessageReceived(CharSequence cs) {
        String text = cs.toString();
        LOG.debug("Websocket message received [{}]", text);
        ExchangeResponse msg = Optional.ofNullable(text).map(this.text2MessageConvertorFunc).orElse(null);
        if (msg == null) {
            LOG.warn("Failed to parse message [{}]", text);
            return;
        }

        String subId = Optional.ofNullable(msg).map(this.msgIdExtractorFunc).orElse(null);
        if (subId == null) {
            LOG.warn("Failed to extract sub id from message");
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
        }
    }

    public void sendMessage(String message) throws IOException {
        LOG.debug("Sending message: {}", message);

        if (message != null) {
            ws().sendText(message, true).join();
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

    @Override
    public void stop() {
        if (!stopped) {
            try {
                ws.abort();
            } catch (NullPointerException e) {
                // fine
            }
            finally {
                pingTask.cancel();
                t.cancel();
            }
        }
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

    static ByteBuffer pingPayload = ByteBuffer.wrap(new byte[]{8, 1, 8, 1});

    public void ping() throws IOException {
        LOG.debug("Sending ping message");

        ws().sendPing(pingPayload).join();
    }
}
