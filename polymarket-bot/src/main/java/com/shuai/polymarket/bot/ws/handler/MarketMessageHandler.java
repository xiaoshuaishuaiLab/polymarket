package com.shuai.polymarket.bot.ws.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuai.polymarket.bot.ws.dispatcher.MarketEventDispatcher;
import com.shuai.polymarket.bot.ws.model.MarketEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses incoming text WebSocket frames as {@link MarketEvent} JSON and
 * delegates to {@link MarketEventDispatcher} for Spring event publishing.
 */
@Slf4j
public class MarketMessageHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final ObjectMapper objectMapper;
    private final MarketEventDispatcher dispatcher;

    public MarketMessageHandler(ObjectMapper objectMapper, MarketEventDispatcher dispatcher) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        if (text == null || text.isBlank()) {
            return;
        }
        // Server sends "PONG" as plain text; PingPongHandler filters it first,
        // but guard here just in case ordering changes.
        if ("PONG".equalsIgnoreCase(text.trim())) {
            return;
        }
        try {
            MarketEvent event = objectMapper.readValue(text, MarketEvent.class);
            dispatcher.dispatch(event);
        } catch (Exception e) {
            log.warn("Failed to parse market event: {} — payload: {}", e.getMessage(), text);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("MarketMessageHandler error on channel {}: {}", ctx.channel().id(), cause.getMessage(), cause);
        ctx.close();
    }
}
