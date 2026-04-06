# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Communication Guidelines

1. Answer questions in Chinese
2. Use English for all code comments

## Build & Run Commands

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Run tests
mvn test

# Run application
java -jar polymarket-bot/target/polymarket-bot-*.jar
```

## Architecture Overview

This is a Spring Boot application that subscribes to Polymarket real-time market data via WebSocket.

### Core Components

```
TokenRegistry              ← Single entry point for add/remove token IDs
      │
      ▼
MarketChannelPool          ← Manages N shard connections (default 50 tokens/shard)
  ├── shard-0 (tokens 0..49)  ──┐
  ├── shard-1 (tokens 50..99) ──┤──► MarketEventDispatcher
  └── shard-N                 ──┘         │
                                    @EventListener beans
```

- **TokenRegistry**: Thread-safe registry for token subscriptions; integrates with web3j for on-chain event-driven subscription changes
- **MarketChannelPool**: Manages multiple `MarketChannelClient` shards; auto-creates new shards when full, shuts down empty ones
- **MarketChannelClient**: Single Netty WebSocket connection with exponential backoff reconnection (1s → 2s → ... → 30s cap)
- **MarketEventDispatcher**: Publishes parsed market events as Spring `ApplicationEvent` for consumption by `@EventListener` beans

### Netty Pipeline (per shard)

```
SslHandler → HttpClientCodec → HttpObjectAggregator
→ WebSocketClientProtocolHandler → WebSocketFrameAggregator
→ PingPongHandler → MarketMessageHandler
```

### Event Types

| `event_type` | Description |
|---|---|
| `book` | Full orderbook snapshot |
| `price_change` | Order placed/cancelled |
| `last_trade_price` | Trade executed |
| `best_bid_ask` | Best bid/ask update |
| `market_resolved` | Market resolved |

## Configuration

Key settings in `polymarket-bot/src/main/resources/application.yml`:

- `polymarket.ws.url`: WebSocket endpoint
- `polymarket.ws.shard-max-size`: Max tokens per connection (default 50)
- `polymarket.ws.initial-tokens`: Token IDs to subscribe on startup

## Java 21 Guidelines

- Prefer JDK 21 features: records, pattern matching for `instanceof`/`switch`, sequenced collections, virtual threads, text blocks
- Use `var` when type is clear
