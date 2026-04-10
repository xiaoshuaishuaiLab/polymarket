package com.shuai.polymarket.bot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * Market Table
 * </p>
 *
 * @author shuai
 * @since 2026-04-10
 */
@Getter
@Setter
@ToString
@TableName("market")
public class Market implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary Key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Market ID (ID returned by API)
     */
    @TableField("market_id")
    private String marketId;

    /**
     * Associated Event ID
     */
    @TableField("event_id")
    private String eventId;

    /**
     * Condition ID
     */
    @TableField("condition_id")
    private String conditionId;

    /**
     * Market Question
     */
    @TableField("question")
    private String question;

    /**
     * URL Slug
     */
    @TableField("slug")
    private String slug;

    /**
     * Resolution Source URL
     */
    @TableField("resolution_source")
    private String resolutionSource;

    /**
     * Image URL
     */
    @TableField("image")
    private String image;

    /**
     * Icon URL
     */
    @TableField("icon")
    private String icon;

    /**
     * Market Description
     */
    @TableField("description")
    private String description;

    /**
     * Outcomes JSON Array
     */
    @TableField("outcomes")
    private String outcomes;

    /**
     * Outcome Prices JSON Array
     */
    @TableField("outcome_prices")
    private String outcomePrices;

    /**
     * CLOB Token IDs JSON Array
     */
    @TableField("clob_token_ids")
    private String clobTokenIds;

    /**
     * Volume
     */
    @TableField("volume")
    private BigDecimal volume;

    /**
     * Liquidity
     */
    @TableField("liquidity")
    private BigDecimal liquidity;

    /**
     * Is Active
     */
    @TableField("active")
    private Boolean active;

    /**
     * Is Closed
     */
    @TableField("closed")
    private Boolean closed;

    /**
     * Is Archived
     */
    @TableField("archived")
    private Boolean archived;

    /**
     * Is New Event
     */
    @TableField("isNew")
    private Boolean isNew;

    /**
     * Is Featured
     */
    @TableField("featured")
    private Boolean featured;

    /**
     * Is Restricted
     */
    @TableField("restricted")
    private Boolean restricted;

    /**
     * Is Order Book Enabled
     */
    @TableField("enable_order_book")
    private Boolean enableOrderBook;

    /**
     * Minimum Price Tick Size
     */
    @TableField("order_price_min_tick_size")
    private BigDecimal orderPriceMinTickSize;

    /**
     * Minimum Order Size
     */
    @TableField("order_min_size")
    private BigDecimal orderMinSize;

    /**
     * Group Item Title
     */
    @TableField("group_item_title")
    private String groupItemTitle;

    /**
     * Group Item Threshold
     */
    @TableField("group_item_threshold")
    private String groupItemThreshold;

    /**
     * Question ID
     */
    @TableField("question_id")
    private String questionId;

    /**
     * Negative Risk Flag
     */
    @TableField("neg_risk")
    private Boolean negRisk;

    /**
     * Negative Risk Market ID
     */
    @TableField("neg_risk_market_id")
    private String negRiskMarketId;

    /**
     * Negative Risk Request ID
     */
    @TableField("neg_risk_request_id")
    private String negRiskRequestId;

    /**
     * Spread
     */
    @TableField("spread")
    private BigDecimal spread;

    /**
     * Best Bid
     */
    @TableField("best_bid")
    private BigDecimal bestBid;

    /**
     * Best Ask
     */
    @TableField("best_ask")
    private BigDecimal bestAsk;

    /**
     * Last Trade Price
     */
    @TableField("last_trade_price")
    private BigDecimal lastTradePrice;

    /**
     * 1 Hour Price Change
     */
    @TableField("one_hour_price_change")
    private BigDecimal oneHourPriceChange;

    /**
     * Competitiveness
     */
    @TableField("competitive")
    private BigDecimal competitive;

    /**
     * UMA Bond
     */
    @TableField("uma_bond")
    private String umaBond;

    /**
     * UMA Reward
     */
    @TableField("uma_reward")
    private String umaReward;

    /**
     * Fee Type
     */
    @TableField("fee_type")
    private String feeType;

    /**
     * Fee Schedule JSON
     */
    @TableField("fee_schedule")
    private String feeSchedule;

    /**
     * Is Accepting Orders
     */
    @TableField("accepting_orders")
    private Boolean acceptingOrders;

    /**
     * Start Time
     */
    @TableField("start_date")
    private LocalDateTime startDate;

    /**
     * End Time
     */
    @TableField("end_date")
    private LocalDateTime endDate;

    /**
     * Creation Time
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * Update Time
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Logical Delete Flag
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
