package com.shuai.polymarket.bot.ws.listener;

import com.shuai.polymarket.bot.ws.model.MarketEventWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketEventLogger {

    @EventListener
    public void onMarketEvent(MarketEventWrapper wrapper) {
//        log.info("MarketEvent received: {}", wrapper.getMarketEvent());
    }
}
