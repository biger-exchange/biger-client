package com.biger.client.ws.react.websocket.netty;

import com.biger.client.ws.react.domain.response.ExchangeResponse;
import com.biger.client.ws.react.websocket.Client;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.*;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

    // for requestSingle
    private final ConcurrentMap<String, MonoSink> ackSinkMap = new ConcurrentHashMap<>();

    private AtomicBoolean started = new AtomicBoolean(false);

    private boolean isManualDisconnect = false;

    @Getter
    final private URI wsUri;

    private volatile Channel websocketChannel;

    @Getter
    private final NioEventLoopGroup eventLoopGroup;

    @Setter
    private boolean acceptAllCertificates;

    final EmitterProcessor<ExchangeResponse> emitter;
    final FluxSink<ExchangeResponse> sink;

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

        this.eventLoopGroup = new NioEventLoopGroup(2);

        emitter = EmitterProcessor.create(256, false);
        sink = emitter.sink(FluxSink.OverflowStrategy.LATEST);

    }

    static class Subscription {
        final String subRequest;
        final String unSubRequest;
        final AtomicInteger count = new AtomicInteger(0);

        public Subscription(String subRequest, String unSubRequest) {
            this.subRequest = subRequest;
            this.unSubRequest = unSubRequest;
        }
    }

    public Mono<Integer> start() {
        if (!this.started.compareAndSet(false, true)) {
            return Mono.just(0);
        }
        return doStart();
    }

    public Mono<Integer> doStart() {

        final BigerWebSocketClientHandler handler = new NettyBigerWebSocketClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                        wsUri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()), this::onMessageReceived);

        eventLoopGroup.scheduleWithFixedDelay(()->ping(), 15, 15, TimeUnit.SECONDS);

        return Mono.<Integer>create(sink -> {

            LOG.info("building bootstrap and connect now");
            String host = wsUri.getHost();
            int port = extractPort(this.wsUri);

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) (this.connectTimeout.toMillis()))
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            SslContext sslContext = buildSslContext(wsUri);
                            if (sslContext != null) {
                                p.addLast(sslContext.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(maxFrameSize),
                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            bootstrap.connect(host, port).addListener((ChannelFuture channelFuture) -> {
                this.websocketChannel = channelFuture.channel();
                if (channelFuture.isSuccess()) {
                    handler.handshakeFuture().addListener(f -> {
                        if (f.isSuccess()) {
                            LOG.warn("Connected to ws server successfully");
                            sink.success(1);
                        } else {
                            LOG.warn("handshake failed", f.cause());
                            handleConnectError(sink, f.cause());
                        }
                    });
                } else {
                    handleConnectError(sink, channelFuture.cause());
                }
            });
        }).doOnError(err -> {
            LOG.warn("Problem with connection", err);
        }).retryWhen((Flux<Throwable> errFlux) -> errFlux.flatMap(x -> {
            LOG.info("Trying to reconnect now");
            return Mono.delay(this.retryTimeout);
        })).doOnSuccess((x) -> {
            this.connectedSuccessfully = true;
            LOG.info("Connection to websocket server succeeded, resub now");
            resub();
        });

    }


    SslContext buildSslContext(URI uri) {
        String scheme = uri.getScheme();
        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        if (!ssl) {
            return null;
        }

        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient();
        if (acceptAllCertificates) {
            sslContextBuilder = sslContextBuilder.trustManager(InsecureTrustManagerFactory.INSTANCE);
        }
        try {
            return sslContextBuilder.build();
        } catch (SSLException ex) {
            throw new IllegalStateException("Failed to parse ssl", ex);
        }

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

    private void onMessageReceived(String text) {
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

        message.setTopic(msgIdExtractorFunc.apply(message));

        sink.next(message);
    }

    private void ensureSocketReady() throws IOException {
        if (this.websocketChannel == null || !this.websocketChannel.isOpen()) {
            LOG.warn("WebSocket is not open! Call connect first.");
            throw new IOException("Websocket is not connected yet");
        }

        if (!this.websocketChannel.isWritable()) {
            LOG.warn("Cannot send data to WebSocket as it is not writable.");
            return;
        }
    }

    public void sendMessage(String message) throws IOException {
        LOG.debug("Sending message: {}", message);
        this.ensureSocketReady();

        if (message != null) {
            WebSocketFrame frame = new TextWebSocketFrame(message);
            this.websocketChannel.writeAndFlush(frame).addListener(future -> {
                LOG.debug("Finished (success [{}]) sending message to network [{}]", future.isSuccess(), message);
            });
        }
    }

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

            });
        }).doOnError(err -> {
            LOG.warn("Got error in request single for requsetId " + requestId, err);
        });
    }

    public void addAckMonoSink(String requestId, MonoSink<ExchangeResponse> monoSink) {
        this.ackSinkMap.putIfAbsent(requestId, monoSink);
    }

    public void stop() {
        if (!this.started.compareAndSet(true,false)) {
            return;
        }

        isManualDisconnect = true;
        connectedSuccessfully = false;
        if (this.websocketChannel.isOpen()) {
            CloseWebSocketFrame closeFrame = new CloseWebSocketFrame();
            websocketChannel.writeAndFlush(closeFrame).addListener(future -> {
                subIdMap.clear();
                eventLoopGroup.shutdownGracefully(2, 30, TimeUnit.SECONDS).addListener(f -> {
                    LOG.info("Disconnected");
                });
            });
        } else {
            LOG.warn("Disconnect called but already disconnected");
        }
    }

    public Flux<ExchangeResponse> sub(String subId, String subRequestMsg, String unSubRequestMsg, Predicate<String> topicFilter, boolean forceSubReq) {
        LOG.debug("Subscribing to websocket Channel {}", subId);

        Subscription sub = new Subscription(subRequestMsg, unSubRequestMsg);

        {
            Subscription prev = this.subIdMap.putIfAbsent(subId, sub);
            if (prev != null) {
                sub = prev;
            }
        }

        Subscription finalSub = sub;
        return emitter.filter(resp->topicFilter.test(resp.getTopic()))
                .doOnSubscribe(s->{
                    if (forceSubReq || finalSub.count.incrementAndGet() == 1) {
                        try {
                            sendMessage(subRequestMsg);
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                })
                .doFinally(signal->{
                            if (finalSub.count.decrementAndGet() == 0) {
                                try {
                                    sendMessage(unSubRequestMsg);
                                } catch (IOException e) {
                                    //ignore
                                }
                            }
                        }
                );
    }

    private void resub() {
        this.subIdMap.entrySet().forEach(x -> {
            if (x.getValue().count.get() > 0) {
                try {
                    sendMessage(x.getValue().subRequest);
                } catch (Exception ex) {
                    LOG.warn("Failed to resub for [{}]", x.getValue(), ex);
                }
            }
        });
    }

    private void handleConnectError(MonoSink sink, Throwable throwable) {
        LOG.error("Websocket client failed to connect ", throwable);
        isManualDisconnect = true;
        this.websocketChannel.deregister();
        ChannelFuture disconnect = websocketChannel.disconnect();
        disconnect.addListener(f -> {
            if (f.isSuccess()) {
                isManualDisconnect = false;
            }
            // shutdown sockets after disconnect for avoiding sockets leak
        });
        sink.error(throwable);
    }


    public void ping() {

        if (this.websocketChannel == null || !this.websocketChannel.isOpen()) {
            return;
        }

        if (!this.websocketChannel.isWritable()) {
            return;
        }

        LOG.debug("Sending ping message");

        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{8, 1, 8, 1}));
        this.websocketChannel.writeAndFlush(frame);
    }

    public void request(String msg) {
        WebSocketFrame frame = new TextWebSocketFrame(msg);
        this.websocketChannel.writeAndFlush(frame);
    }

    @ChannelHandler.Sharable
    class NettyBigerWebSocketClientHandler extends BigerWebSocketClientHandler {
        protected NettyBigerWebSocketClientHandler(WebSocketClientHandshaker handshaker, Consumer<String> consumer) {
            super(handshaker, consumer);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (isManualDisconnect) {
                isManualDisconnect = false;
            } else {
                super.channelInactive(ctx);
                if (connectedSuccessfully) {
                    LOG.info("Reopening websocket because it was closed by the host");
                    try {
                        websocketChannel.deregister();
                        websocketChannel.disconnect();
                    } catch (Exception ex) {
                        LOG.warn("Failed to deregister & disconnect");
                    }
                    eventLoopGroup.schedule(() -> doStart().subscribe(), retryTimeout.toMillis(), TimeUnit.MILLISECONDS).addListener((x) -> {
                        LOG.info("reconnect scheduled by event loop, x [{}]", x);
                    });
                }
            }
        }
    }

}
