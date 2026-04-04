package com.shuai.polymarket.bot.ws.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuai.polymarket.bot.ws.config.MarketWsProperties;
import com.shuai.polymarket.bot.ws.dispatcher.MarketEventDispatcher;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a pool of {@link MarketChannelClient} shards.
 *
 * <p>Each shard holds at most {@code shard-max-size} token IDs on a dedicated
 * WebSocket connection. When all existing shards are full a new one is created
 * automatically. Empty shards are shut down and removed.
 *
 * <p>All public methods are thread-safe.
 */
@Slf4j
@Component
public class MarketChannelPool {

    private final MarketWsProperties properties;
    private final ObjectMapper objectMapper;
    private final MarketEventDispatcher dispatcher;

    /** Shared Netty event-loop group for all shards */
    private final NioEventLoopGroup eventLoopGroup;

    /** shardId → client */
    private final Map<String, MarketChannelClient> shards = new ConcurrentHashMap<>();

    /** tokenId → shardId (fast reverse lookup) */
    private final Map<String, String> tokenToShard = new ConcurrentHashMap<>();

    private final AtomicInteger shardCounter = new AtomicInteger(0);

    public MarketChannelPool(MarketWsProperties properties,
                             ObjectMapper objectMapper,
                             MarketEventDispatcher dispatcher) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
        // Use 2× available processors; all shards share this group
        this.eventLoopGroup = new NioEventLoopGroup(
                Math.max(2, Runtime.getRuntime().availableProcessors() * 2));
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Subscribes a token ID. Finds a shard with capacity or creates a new one.
     * No-op if the token is already subscribed.
     */
    public synchronized void subscribe(String tokenId) {
        if (tokenToShard.containsKey(tokenId)) {
            log.debug("Token {} already subscribed in shard {}", tokenId, tokenToShard.get(tokenId));
            return;
        }
        MarketChannelClient shard = findOrCreateShard();
        shard.addToken(tokenId);
        tokenToShard.put(tokenId, shard.getShardId());
        log.info("Subscribed token {} to shard {} ({} tokens)", tokenId, shard.getShardId(), shard.tokenCount());
    }

    /**
     * Unsubscribes a token ID. Removes the token from its shard and shuts down
     * the shard if it becomes empty.
     */
    public synchronized void unsubscribe(String tokenId) {
        String shardId = tokenToShard.remove(tokenId);
        if (shardId == null) {
            log.debug("Token {} not found in any shard", tokenId);
            return;
        }
        MarketChannelClient shard = shards.get(shardId);
        if (shard == null) {
            return;
        }
        shard.removeToken(tokenId);
        log.info("Unsubscribed token {} from shard {} ({} tokens remaining)", tokenId, shardId, shard.tokenCount());
        if (shard.isEmpty()) {
            log.info("Shard {} is empty, shutting it down", shardId);
            shard.stop();
            shards.remove(shardId);
        }
    }

    /** Returns a snapshot of all currently subscribed token IDs. */
    public List<String> subscribedTokens() {
        return new ArrayList<>(tokenToShard.keySet());
    }

    /** Returns the current number of active shard connections. */
    public int shardCount() {
        return shards.size();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Finds a shard that has room for one more token, or creates a new shard.
     */
    private MarketChannelClient findOrCreateShard() {
        for (MarketChannelClient client : shards.values()) {
            if (client.tokenCount() < properties.getShardMaxSize()) {
                return client;
            }
        }
        return createShard();
    }

    private MarketChannelClient createShard() {
        String shardId = "shard-" + shardCounter.getAndIncrement();
        MarketChannelClient client = new MarketChannelClient(
                shardId, properties, eventLoopGroup, objectMapper, dispatcher);
        shards.put(shardId, client);
        client.start();
        log.info("Created new shard: {}", shardId);
        return client;
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down MarketChannelPool ({} shards)", shards.size());
        shards.values().forEach(MarketChannelClient::stop);
        shards.clear();
        tokenToShard.clear();
        eventLoopGroup.shutdownGracefully();
    }
}
