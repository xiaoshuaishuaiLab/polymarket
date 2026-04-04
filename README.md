# Polymarket Bot

A Spring Boot application that subscribes to [Polymarket](https://polymarket.com) real-time market data via WebSocket, built on **Netty** for high-performance async I/O.

## Features

- **Netty-based WSS client** — non-blocking WebSocket connection to `wss://ws-subscriptions-clob.polymarket.com/ws/market`
- **Shard connection pool** — splits large token sets across multiple independent WebSocket connections (configurable shard size, default 50 tokens/connection)
- **Incremental subscriptions** — add or remove individual token IDs at runtime without reconnecting
- **Chain-event driven** — integrates with [web3j](https://docs.web3j.io/) to react to on-chain events and dynamically update subscriptions
- **Auto-reconnect** — exponential backoff reconnection (1s → 2s → 4s → … → 30s cap), replays full subscription on reconnect
- **Heartbeat** — sends `PING` every 10 seconds, filters server `PONG` responses
- **Spring event bus** — parsed market events are published as Spring `ApplicationEvent`, consumed by any `@EventListener` bean

## Architecture

```
On-chain event (web3j)
        │
        ▼
  TokenRegistry          ← single entry point for add/remove token IDs
        │
        ▼
MarketChannelPool        ← manages N shard connections
  ├── shard-0  (token 0..49)   ──┐
  ├── shard-1  (token 50..99)  ──┤──► MarketEventDispatcher
  └── shard-N  (token N*50..)  ──┘         │
                                     @EventListener beans
                                     (e.g. MarketEventLogger, strategy modules)
```

Each shard is a `MarketChannelClient` with its own Netty pipeline:

```
SslHandler → HttpClientCodec → HttpObjectAggregator
→ WebSocketClientProtocolHandler → WebSocketFrameAggregator
→ PingPongHandler → MarketMessageHandler
```

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5 |
| Network | Netty 4.2 |
| Chain | web3j 4.14 |
| Build | Maven |

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+

### Build

```bash
mvn clean package -DskipTests
```

### Run

```bash
java -jar polymarket-bot/target/polymarket-bot-*.jar
```

On startup the application connects to the Polymarket WSS endpoint and subscribes to the token IDs listed under `polymarket.ws.initial-tokens`.

## Configuration

All settings live in [`application.yml`](polymarket-bot/src/main/resources/application.yml):

```yaml
polymarket:
  ws:
    url: wss://ws-subscriptions-clob.polymarket.com/ws/market
    shard-max-size: 50          # max tokens per WebSocket connection
    reconnect-initial-delay: 1000  # ms
    reconnect-max-delay: 30000     # ms
    ping-interval: 10000           # ms
    initial-tokens:
      - "<token_id_1>"
      - "<token_id_2>"
```

| Property | Default | Description |
|---|---|---|
| `url` | Polymarket WSS endpoint | WebSocket server address |
| `shard-max-size` | `50` | Maximum token IDs per connection |
| `reconnect-initial-delay` | `1000` | First reconnect wait (ms) |
| `reconnect-max-delay` | `30000` | Reconnect backoff cap (ms) |
| `ping-interval` | `10000` | Heartbeat interval (ms) |
| `initial-tokens` | `[]` | Token IDs to subscribe on startup |

## Dynamic Subscription at Runtime

Inject `TokenRegistry` into any Spring bean to add or remove tokens while the application is running:

```java
@Autowired
private TokenRegistry tokenRegistry;

// Subscribe a new token (e.g. from a web3j event callback)
tokenRegistry.onTokenAdded(tokenId);

// Unsubscribe a token (e.g. market resolved on-chain)
tokenRegistry.onTokenRemoved(tokenId);

// Bulk seed
tokenRegistry.addAll(List.of(tokenId1, tokenId2));

// Replace entire set (diff-based: only sends incremental messages)
tokenRegistry.replaceAll(newTokenIds);
```

The pool automatically creates a new shard connection when all existing shards are full, and shuts down empty shards when tokens are removed.

## Consuming Market Events

Implement any Spring bean with `@EventListener` to receive parsed events:

```java
@Component
public class MyStrategy {

    @EventListener
    public void onMarketEvent(MarketEventWrapper wrapper) {
        MarketEvent event = wrapper.getMarketEvent();
        switch (event.getEventType()) {
            case "book"             -> handleBook(event);
            case "price_change"     -> handlePriceChange(event);
            case "last_trade_price" -> handleTrade(event);
            case "best_bid_ask"     -> handleBestBidAsk(event);
            case "market_resolved"  -> handleResolved(event);
        }
    }
}
```

### Supported Event Types

| `event_type` | Description |
|---|---|
| `book` | Full orderbook snapshot (on subscribe and after trades) |
| `price_change` | Order placed or cancelled — level-2 price update |
| `last_trade_price` | Maker/taker order matched |
| `best_bid_ask` | Best bid/ask update (`custom_feature_enabled: true`) |
| `new_market` | New market created (`custom_feature_enabled: true`) |
| `market_resolved` | Market resolved (`custom_feature_enabled: true`) |

## Project Structure

```
polymarket-bot/src/main/java/com/shuai/polymarket/bot/
├── PolymarketBotApplication.java
└── ws/
    ├── config/
    │   ├── MarketWsProperties.java      # @ConfigurationProperties binding
    │   └── MarketWsStartupRunner.java   # subscribes initial-tokens on startup
    ├── client/
    │   ├── MarketChannelClient.java     # single Netty WSS connection + reconnect
    │   └── MarketChannelPool.java       # shard pool, thread-safe token management
    ├── handler/
    │   ├── PingPongHandler.java         # heartbeat
    │   └── MarketMessageHandler.java    # JSON parse → dispatcher
    ├── model/
    │   ├── MarketEvent.java             # unified event POJO
    │   └── MarketEventWrapper.java      # Spring ApplicationEvent wrapper
    ├── dispatcher/
    │   └── MarketEventDispatcher.java   # publishes events via ApplicationEventPublisher
    ├── registry/
    │   └── TokenRegistry.java           # web3j bridge, add/remove tokens
    └── listener/
        └── MarketEventLogger.java       # logs all received events (example consumer)
```

## License

MIT
