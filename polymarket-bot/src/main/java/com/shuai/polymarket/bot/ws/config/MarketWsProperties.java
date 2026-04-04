package com.shuai.polymarket.bot.ws.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "polymarket.ws")
public class MarketWsProperties {

    /** WSS endpoint */
    private String url = "wss://ws-subscriptions-clob.polymarket.com/ws/market";

    /** Max number of token IDs per WebSocket shard connection */
    private int shardMaxSize = 50;

    /** Initial reconnect delay in milliseconds */
    private long reconnectInitialDelay = 1000L;

    /** Maximum reconnect delay in milliseconds (exponential backoff cap) */
    private long reconnectMaxDelay = 30000L;

    /** PING interval in milliseconds */
    private long pingInterval = 10000L;

    /** Token IDs to subscribe on startup */
    private List<String> initialTokens = new ArrayList<>();
}
