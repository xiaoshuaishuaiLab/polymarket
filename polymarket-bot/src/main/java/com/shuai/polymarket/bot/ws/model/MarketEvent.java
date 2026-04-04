package com.shuai.polymarket.bot.ws.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Unified POJO for all Polymarket market channel events.
 * Fields are populated based on the {@code event_type} value.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketEvent {

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("asset_id")
    private String assetId;

    /** Used in new_market / market_resolved events */
    @JsonProperty("assets_ids")
    private List<String> assetsIds;

    private String market;
    private String timestamp;

    // --- book ---
    private List<PriceLevel> bids;
    private List<PriceLevel> asks;
    private String hash;

    // --- price_change ---
    @JsonProperty("price_changes")
    private List<PriceChange> priceChanges;

    // --- tick_size_change ---
    @JsonProperty("old_tick_size")
    private String oldTickSize;

    @JsonProperty("new_tick_size")
    private String newTickSize;

    // --- last_trade_price ---
    private String price;
    private String side;
    private String size;

    @JsonProperty("fee_rate_bps")
    private String feeRateBps;

    // --- best_bid_ask ---
    @JsonProperty("best_bid")
    private String bestBid;

    @JsonProperty("best_ask")
    private String bestAsk;

    private String spread;

    // --- new_market / market_resolved ---
    private String id;
    private String question;
    private String slug;
    private String description;
    private List<String> outcomes;

    @JsonProperty("winning_asset_id")
    private String winningAssetId;

    @JsonProperty("winning_outcome")
    private String winningOutcome;

    @JsonProperty("condition_id")
    private String conditionId;

    private Boolean active;

    @JsonProperty("clob_token_ids")
    private List<String> clobTokenIds;

    @JsonProperty("order_price_min_tick_size")
    private String orderPriceMinTickSize;

    @JsonProperty("group_item_title")
    private String groupItemTitle;

    @JsonProperty("tags")
    private List<String> tags;

    // ---- nested types ----

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceLevel {
        private String price;
        private String size;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceChange {
        @JsonProperty("asset_id")
        private String assetId;
        private String price;
        private String size;
        private String side;
        private String hash;
        @JsonProperty("best_bid")
        private String bestBid;
        @JsonProperty("best_ask")
        private String bestAsk;
    }
}
