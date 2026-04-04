package com.shuai.polymarket.bot.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuai.polymarket.bot.ws.config.MarketWsProperties;
import com.shuai.polymarket.bot.ws.dispatcher.MarketEventDispatcher;
import com.shuai.polymarket.bot.ws.handler.MarketMessageHandler;
import com.shuai.polymarket.bot.ws.handler.PingPongHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A single Netty WebSocket client connection to the Polymarket market channel.
 * Each instance manages a fixed shard of token IDs and handles:
 * <ul>
 *   <li>Initial connection and full subscription on connect</li>
 *   <li>Incremental subscribe / unsubscribe without reconnecting</li>
 *   <li>Exponential-backoff reconnection on disconnect</li>
 * </ul>
 */
@Slf4j
public class MarketChannelClient {

    private static final String SUBSCRIBE_OP = "subscribe";
    private static final String UNSUBSCRIBE_OP = "unsubscribe";

    private final String shardId;

    public String getShardId() {
        return shardId;
    }

    private final MarketWsProperties properties;
    private final NioEventLoopGroup eventLoopGroup;
    private final ObjectMapper objectMapper;
    private final MarketEventDispatcher dispatcher;

    /** Token IDs currently managed by this shard */
    private final Set<String> tokenIds = ConcurrentHashMap.newKeySet();

    private volatile Channel channel;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long reconnectDelay;

    public MarketChannelClient(String shardId,
                               MarketWsProperties properties,
                               NioEventLoopGroup eventLoopGroup,
                               ObjectMapper objectMapper,
                               MarketEventDispatcher dispatcher) {
        this.shardId = shardId;
        this.properties = properties;
        this.eventLoopGroup = eventLoopGroup;
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        this.reconnectDelay = properties.getReconnectInitialDelay();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() {
        if (running.compareAndSet(false, true)) {
            connect();
        }
    }

    public void stop() {
        running.set(false);
        if (channel != null) {
            channel.close();
        }
    }

    // -------------------------------------------------------------------------
    // Token management
    // -------------------------------------------------------------------------

    public int tokenCount() {
        return tokenIds.size();
    }

    public boolean containsToken(String tokenId) {
        return tokenIds.contains(tokenId);
    }

    /**
     * Adds a token to this shard. If the connection is active, sends an incremental
     * subscribe message; otherwise the token will be included in the next full
     * subscription sent after reconnect.
     */
    public void addToken(String tokenId) {
        if (tokenIds.add(tokenId) && isConnected()) {
            sendSubscriptionOp(List.of(tokenId), SUBSCRIBE_OP);
        }
    }

    /**
     * Removes a token from this shard. Sends an incremental unsubscribe if connected.
     */
    public void removeToken(String tokenId) {
        if (tokenIds.remove(tokenId) && isConnected()) {
            sendSubscriptionOp(List.of(tokenId), UNSUBSCRIBE_OP);
        }
    }

    public boolean isEmpty() {
        return tokenIds.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Internal connection logic
    // -------------------------------------------------------------------------

    private void connect() {
        if (!running.get()) {
            return;
        }
        URI uri;
        try {
            uri = new URI(properties.getUrl());
        } catch (Exception e) {
            log.error("[{}] Invalid WSS URL: {}", shardId, properties.getUrl(), e);
            return;
        }

        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 443;
        boolean ssl = "wss".equalsIgnoreCase(uri.getScheme());

        SslContext sslContext = null;
        if (ssl) {
            try {
                sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } catch (Exception e) {
                log.error("[{}] Failed to build SSL context", shardId, e);
                return;
            }
        }

        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());

        SslContext finalSslContext = sslContext;
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        if (finalSslContext != null) {
                            p.addLast(finalSslContext.newHandler(ch.alloc(), host, port));
                        }
                        p.addLast(new HttpClientCodec());
                        p.addLast(new HttpObjectAggregator(65536));
                        p.addLast(new WebSocketClientProtocolHandler(handshaker));
                        p.addLast(new WebSocketFrameAggregator(65536));
                        p.addLast(new PingPongHandler(properties.getPingInterval()));
                        p.addLast(new MarketMessageHandler(objectMapper, dispatcher));
                        p.addLast(buildReconnectHandler());
                    }
                });

        log.info("[{}] Connecting to {}:{}", shardId, host, port);
        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                reconnectDelay = properties.getReconnectInitialDelay();
                log.info("[{}] Connected, waiting for WS handshake", shardId);
                // Send full subscription after handshake completes
                channel.pipeline()
                        .fireUserEventTriggered(WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE);
            } else {
                log.warn("[{}] Connect failed: {}", shardId, future.cause().getMessage());
                scheduleReconnect();
            }
        });
    }

    /**
     * Inbound handler that detects channel close and triggers reconnect,
     * and also listens for the WS handshake-complete event to send the initial subscription.
     */
    private ChannelInboundHandlerAdapter buildReconnectHandler() {
        return new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                    log.info("[{}] WebSocket handshake complete", shardId);
                    sendFullSubscription(ctx.channel());
                }
                super.userEventTriggered(ctx, evt);
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                log.warn("[{}] Channel inactive, scheduling reconnect", shardId);
                scheduleReconnect();
                super.channelInactive(ctx);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                log.error("[{}] Channel exception: {}", shardId, cause.getMessage(), cause);
                ctx.close();
            }
        };
    }

    private void scheduleReconnect() {
        if (!running.get()) {
            return;
        }
        long delay = reconnectDelay;
        reconnectDelay = Math.min(reconnectDelay * 2, properties.getReconnectMaxDelay());
        log.info("[{}] Reconnecting in {}ms", shardId, delay);
        eventLoopGroup.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
    }

    // -------------------------------------------------------------------------
    // Subscription helpers
    // -------------------------------------------------------------------------

    /**
     * Sends the initial full subscription message with all current token IDs
     * plus the required {@code type} and {@code custom_feature_enabled} fields.
     */
    private void sendFullSubscription(Channel ch) {
        if (tokenIds.isEmpty()) {
            log.debug("[{}] No tokens to subscribe on connect", shardId);
            return;
        }
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("assets_ids", new ArrayList<>(tokenIds));
            msg.put("type", "market");
            msg.put("custom_feature_enabled", true);
            String json = objectMapper.writeValueAsString(msg);
            ch.writeAndFlush(new TextWebSocketFrame(json));
            log.info("[{}] Sent full subscription for {} tokens", shardId, tokenIds.size());
        } catch (Exception e) {
            log.error("[{}] Failed to send full subscription", shardId, e);
        }
    }

    /**
     * Sends an incremental subscribe or unsubscribe message for the given token IDs.
     */
    private void sendSubscriptionOp(List<String> ids, String operation) {
        if (!isConnected() || ids.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("assets_ids", ids);
            msg.put("operation", operation);
            String json = objectMapper.writeValueAsString(msg);
            channel.writeAndFlush(new TextWebSocketFrame(json));
            log.debug("[{}] Sent {} for tokens: {}", shardId, operation, ids);
        } catch (Exception e) {
            log.error("[{}] Failed to send {} operation", shardId, operation, e);
        }
    }

    private boolean isConnected() {
        return channel != null && channel.isActive();
    }
}
