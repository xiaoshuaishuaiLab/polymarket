package com.shuai.polymarket.bot.ws.dispatcher;

import com.shuai.polymarket.bot.ws.model.MarketEvent;
import com.shuai.polymarket.bot.ws.model.MarketEventWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes parsed {@link MarketEvent} objects as Spring {@link MarketEventWrapper} events.
 * Any bean can consume them with {@code @EventListener(MarketEventWrapper.class)}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketEventDispatcher {

    private final ApplicationEventPublisher eventPublisher;

    public void dispatch(MarketEvent event) {
        if (event == null) {
            return;
        }
        log.debug("Dispatching market event: type={} assetId={}", event.getEventType(), event.getAssetId());
        eventPublisher.publishEvent(new MarketEventWrapper(this, event));
    }
}
