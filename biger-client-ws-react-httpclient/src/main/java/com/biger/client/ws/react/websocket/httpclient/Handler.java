package com.biger.client.ws.react.websocket.httpclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class Handler implements Listener {
    final static Logger log = LoggerFactory.getLogger(Handler.class);

    final Consumer<CharSequence> onMessageReceived;

    public Handler(Consumer<CharSequence> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if(log.isDebugEnabled())
            log.debug("WebSocket Client received closing");
        return null;
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        if(log.isDebugEnabled())
            log.debug("WebSocket Client received pong");
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        if(log.isDebugEnabled())
            log.debug("WebSocket Client received message {}", data);
        try {
            this.onMessageReceived.accept(data);
        } catch (Exception ex) {
            log.warn("Failed to consume message [{}]", data, ex);
        }
        webSocket.request(1);
        return null;
    }
}
