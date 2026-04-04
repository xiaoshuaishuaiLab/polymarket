package com.shuai.polymarket.bot.ws.config;

import com.shuai.polymarket.bot.ws.registry.TokenRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Subscribes the configured initial token IDs as soon as the application context is ready.
 * Additional tokens can be added at runtime via {@link TokenRegistry}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketWsStartupRunner implements ApplicationRunner {

    private final MarketWsProperties properties;
    private final TokenRegistry tokenRegistry;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getInitialTokens().isEmpty()) {
            log.info("No initial tokens configured, skipping startup subscription");
            return;
        }
        log.info("Subscribing {} initial token(s) from configuration", properties.getInitialTokens().size());
        tokenRegistry.addAll(properties.getInitialTokens());
    }
}
