package com.shuai.polymarket.bot.ws.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * Sends a text "PING" frame every {@code pingIntervalMs} milliseconds and
 * handles the server's "PONG" response to keep the connection alive.
 */
@Slf4j
public class PingPongHandler extends ChannelInboundHandlerAdapter {

    private final long pingIntervalMs;
    private ScheduledFuture<?> pingTask;

    public PingPongHandler(long pingIntervalMs) {
        this.pingIntervalMs = pingIntervalMs;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        schedulePing(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        cancelPing();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TextWebSocketFrame frame) {
            String text = frame.text();
            if ("PONG".equalsIgnoreCase(text.trim())) {
                log.trace("Received PONG from server");
                frame.release();
                return;
            }
        } else if (msg instanceof PongWebSocketFrame) {
            log.trace("Received binary PONG from server");
            ((PongWebSocketFrame) msg).release();
            return;
        }
        super.channelRead(ctx, msg);
    }

    private void schedulePing(ChannelHandlerContext ctx) {
        pingTask = ctx.executor().scheduleAtFixedRate(() -> {
            if (ctx.channel().isActive()) {
                log.trace("Sending PING");
                ctx.writeAndFlush(new TextWebSocketFrame("PING"));
            }
        }, pingIntervalMs, pingIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void cancelPing() {
        if (pingTask != null && !pingTask.isCancelled()) {
            pingTask.cancel(false);
        }
    }
}
