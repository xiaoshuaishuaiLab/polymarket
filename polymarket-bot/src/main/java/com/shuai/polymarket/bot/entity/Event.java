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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * Event Table
 * </p>
 *
 * @author shuai
 * @since 2026-04-10
 */
@Getter
@Setter
@ToString
@TableName("event")
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary Key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Event ID (ID returned by API)
     */
    @TableField("event_id")
    private String eventId;

    /**
     * Associated Series ID
     */
    @TableField("series_id")
    private String seriesId;

    /**
     * Event Ticker
     */
    @TableField("ticker")
    private String ticker;

    /**
     * URL Slug
     */
    @TableField("slug")
    private String slug;

    /**
     * Event Title
     */
    @TableField("title")
    private String title;

    /**
     * Event Description
     */
    @TableField("description")
    private String description;

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
     * Liquidity
     */
    @TableField("liquidity")
    private BigDecimal liquidity;

    /**
     * Volume
     */
    @TableField("volume")
    private BigDecimal volume;

    /**
     * Open Interest
     */
    @TableField("open_interest")
    private BigDecimal openInterest;

    /**
     * Competitiveness
     */
    @TableField("competitive")
    private BigDecimal competitive;

    /**
     * 24 Hour Volume
     */
    @TableField("volume_24hr")
    private BigDecimal volume24hr;

    /**
     * 1 Week Volume
     */
    @TableField("volume_1wk")
    private BigDecimal volume1wk;

    /**
     * 1 Month Volume
     */
    @TableField("volume_1mo")
    private BigDecimal volume1mo;

    /**
     * 1 Year Volume
     */
    @TableField("volume_1yr")
    private BigDecimal volume1yr;

    /**
     * Is Order Book Enabled
     */
    @TableField("enable_order_book")
    private Boolean enableOrderBook;

    /**
     * CLOB Liquidity
     */
    @TableField("liquidity_clob")
    private BigDecimal liquidityClob;

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
     * Comment Count
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * Event Date
     */
    @TableField("event_date")
    private LocalDate eventDate;

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
