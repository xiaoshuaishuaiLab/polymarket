package com.shuai.polymarket.bot.ws.registry;

import com.shuai.polymarket.bot.ws.client.MarketChannelPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Entry point for registering and deregistering Polymarket token IDs that
 * originate from on-chain events (via web3j).
 *
 * <p>Call {@link #onTokenAdded(String)} / {@link #onTokenRemoved(String)} from
 * your web3j event listener whenever the on-chain state changes. The registry
 * delegates to {@link MarketChannelPool} which handles shard assignment and the
 * actual WebSocket subscribe / unsubscribe messages.
 *
 * <p>Example usage inside a web3j event listener:
 * <pre>{@code
 * contract.someEventFlowable(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
 *     .subscribe(event -> {
 *         tokenRegistry.onTokenAdded(event.tokenId);
 *     });
 * }</pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRegistry {

    private final MarketChannelPool pool;

    // -------------------------------------------------------------------------
    // Single-token operations (called from web3j event callbacks)
    // -------------------------------------------------------------------------

    /**
     * Called when a new token ID appears on-chain and should be subscribed.
     */
    public void onTokenAdded(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            log.warn("Ignoring blank tokenId in onTokenAdded");
            return;
        }
        log.info("Chain event: adding token {}", tokenId);
        pool.subscribe(tokenId);
    }

    /**
     * Called when a token ID is no longer relevant (e.g. market resolved on-chain)
     * and should be unsubscribed.
     */
    public void onTokenRemoved(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            log.warn("Ignoring blank tokenId in onTokenRemoved");
            return;
        }
        log.info("Chain event: removing token {}", tokenId);
        pool.unsubscribe(tokenId);
    }

    // -------------------------------------------------------------------------
    // Bulk operations (e.g. initial seed from a REST snapshot)
    // -------------------------------------------------------------------------

    /**
     * Subscribes a batch of token IDs at once. Useful for seeding the registry
     * from a REST API snapshot on startup.
     */
    public void addAll(Collection<String> tokenIds) {
        if (tokenIds == null || tokenIds.isEmpty()) {
            return;
        }
        log.info("Bulk adding {} tokens", tokenIds.size());
        tokenIds.forEach(this::onTokenAdded);
    }

    /**
     * Unsubscribes a batch of token IDs.
     */
    public void removeAll(Collection<String> tokenIds) {
        if (tokenIds == null || tokenIds.isEmpty()) {
            return;
        }
        log.info("Bulk removing {} tokens", tokenIds.size());
        tokenIds.forEach(this::onTokenRemoved);
    }

    /**
     * Replaces the entire subscribed set with {@code newTokenIds}.
     * Tokens not in the new set are unsubscribed; new tokens are subscribed.
     */
    public void replaceAll(Collection<String> newTokenIds) {
        List<String> current = pool.subscribedTokens();
        List<String> toRemove = current.stream()
                .filter(t -> !newTokenIds.contains(t))
                .toList();
        List<String> toAdd = newTokenIds.stream()
                .filter(t -> !current.contains(t))
                .toList();
        log.info("replaceAll: removing {} tokens, adding {} tokens", toRemove.size(), toAdd.size());
        toRemove.forEach(this::onTokenRemoved);
        toAdd.forEach(this::onTokenAdded);
    }

    /** Returns a snapshot of all currently subscribed token IDs. */
    public List<String> subscribedTokens() {
        return pool.subscribedTokens();
    }

    /** Returns the current number of active shard connections. */
    public int shardCount() {
        return pool.shardCount();
    }
}
