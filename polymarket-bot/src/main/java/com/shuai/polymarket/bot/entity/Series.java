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
 * Event Series Table
 * </p>
 *
 * @author shuai
 * @since 2026-04-10
 */
@Getter
@Setter
@ToString
@TableName("series")
public class Series implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary Key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Series ID (ID returned by API)
     */
    @TableField("series_id")
    private String seriesId;

    /**
     * Series Ticker
     */
    @TableField("ticker")
    private String ticker;

    /**
     * URL Slug
     */
    @TableField("slug")
    private String slug;

    /**
     * Series Title
     */
    @TableField("title")
    private String title;

    /**
     * Series Type
     */
    @TableField("series_type")
    private String seriesType;

    /**
     * Recurrence Type (daily/weekly, etc.)
     */
    @TableField("recurrence")
    private String recurrence;

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
     * Total Volume
     */
    @TableField("volume")
    private BigDecimal volume;

    /**
     * 24 Hour Volume
     */
    @TableField("volume_24hr")
    private BigDecimal volume24hr;

    /**
     * Liquidity
     */
    @TableField("liquidity")
    private BigDecimal liquidity;

    /**
     * Comment Count
     */
    @TableField("comment_count")
    private Integer commentCount;

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
