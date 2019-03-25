package com.biger.client.ws.react.websocket.netty;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class BigerWebSocketClientHandler extends SimpleChannelInboundHandler<Object> {
    static final Logger LOG = LoggerFactory.getLogger(BigerWebSocketClientHandler.class);

    private final WebSocketClientHandshaker handshaker;

    private ChannelPromise handshakeFuture;

    private final Consumer<String> messageHandler;

    public BigerWebSocketClientHandler(WebSocketClientHandshaker handshaker, Consumer<String> messageHandler) {
        this.handshaker = handshaker;
        this.messageHandler = messageHandler;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.warn("WebSocket conn disconnected, going to re-conn if required!");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                LOG.debug("WebSocket Client connected!");
                handshakeFuture.setSuccess();
            } catch (WebSocketHandshakeException e) {
                LOG.warn("WebSocket Client failed to connect", e);
                handshakeFuture.setFailure(e);
            }
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(StandardCharsets.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String text = textFrame.text();
            LOG.debug("WebSocket Client received message: " + text);
            try {
                this.messageHandler.accept(text);
            }catch (Exception ex) {
                LOG.warn("Failed to consume message [{}]", text, ex);
            }
        } else if (frame instanceof PongWebSocketFrame) {
            LOG.debug("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            LOG.debug("WebSocket Client received closing");
            ch.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Got exception in websocket client, ", cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }
}
