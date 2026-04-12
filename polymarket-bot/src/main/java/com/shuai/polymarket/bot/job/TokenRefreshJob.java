package com.shuai.polymarket.bot.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shuai.polymarket.bot.ws.registry.TokenRegistry;
import common.HttpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Periodically fetches active daily-temperature markets from the Gamma API
 * and synchronizes the WSS token subscriptions via TokenRegistry.replaceAll().
 *
 * Query filter: tag_id=103040 (Daily Temperature) & recurrence=daily & closed=false
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRefreshJob {

    private static final String API_PATH = "/events/keyset";
    private static final int LIMIT = 100;
    private static final String TAG_ID = "103040";
    private static final String RECURRENCE = "daily";
    private static final int MAX_TOKEN_IDS = 1000;

    @Value("${polymarket.gamma.api-url}")
    private String apiBaseUrl;

    private final TokenRegistry tokenRegistry;

    @Scheduled(fixedRate = 300_000)
    public void refreshTokens() {
        log.info("TokenRefreshJob started");
        Set<String> tokenIds = new LinkedHashSet<>();
        String afterCursor = null;

        try {
            while (true) {
                String url = buildUrl(afterCursor);
                log.info("Fetching active markets from: {}", url);

                var responseOpt = HttpUtil.get(url);
                if (responseOpt.isEmpty()) {
                    log.warn("Empty response from API, stopping refresh");
                    break;
                }

                JSONObject body = JSON.parseObject(responseOpt.get());
                JSONArray events = body.getJSONArray("events");
                if (events == null || events.isEmpty()) {
                    log.info("No events returned, stopping refresh");
                    break;
                }

                for (int i = 0; i < events.size(); i++) {
                    JSONArray markets = events.getJSONObject(i).getJSONArray("markets");
                    if (markets == null) continue;
                    for (int j = 0; j < markets.size(); j++) {
                        String raw = markets.getJSONObject(j).getString("clobTokenIds");
                        if (raw != null && !raw.isBlank()) {
                            JSON.parseArray(raw, String.class).forEach(tokenIds::add);
                        }
                        if (tokenIds.size() > MAX_TOKEN_IDS) {
                            log.error("Token ID count exceeds safety limit {} during fetch, aborting", MAX_TOKEN_IDS);
                            return;
                        }
                    }
                }

                String nextCursor = body.getString("next_cursor");
                if (nextCursor == null || nextCursor.isBlank()) break;
                afterCursor = nextCursor;
            }
        } catch (Exception e) {
            log.error("TokenRefreshJob encountered an error", e);
            return;
        }

        log.info("TokenRefreshJob fetched {} token IDs", tokenIds.size());

        if (tokenIds.isEmpty()) {
            log.warn("No token IDs found, skipping replaceAll to avoid unsubscribing everything");
            return;
        }

        tokenRegistry.replaceAll(tokenIds);
    }

    private String buildUrl(String afterCursor) {
        var sb = new StringBuilder(apiBaseUrl)
                .append(API_PATH)
                .append("?limit=").append(LIMIT)
                .append("&order=startDate")
                .append("&ascending=false")
                .append("&tag_id=").append(TAG_ID)
                .append("&recurrence=").append(RECURRENCE)
                .append("&closed=false");
        if (afterCursor != null && !afterCursor.isBlank()) {
            sb.append("&after_cursor=").append(afterCursor);
        }
        return sb.toString();
    }
}
