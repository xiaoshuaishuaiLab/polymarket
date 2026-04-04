package com.shuai.polymarket.bot.ws.model;

import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent wrapper for a {@link MarketEvent}.
 * Consumers use {@code @EventListener} to receive these.
 */
public class MarketEventWrapper extends ApplicationEvent {

    private final MarketEvent marketEvent;

    public MarketEventWrapper(Object source, MarketEvent marketEvent) {
        super(source);
        this.marketEvent = marketEvent;
    }

    public MarketEvent getMarketEvent() {
        return marketEvent;
    }
}
