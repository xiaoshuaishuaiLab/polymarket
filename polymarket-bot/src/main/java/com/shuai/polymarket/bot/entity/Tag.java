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
import java.time.LocalDateTime;

/**
 * <p>
 * Tag Table
 * </p>
 *
 * @author shuai
 * @since 2026-04-10
 */
@Getter
@Setter
@ToString
@TableName("tag")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary Key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Tag ID (ID returned by API)
     */
    @TableField("tag_id")
    private String tagId;

    /**
     * Tag Label
     */
    @TableField("label")
    private String label;

    /**
     * URL Slug
     */
    @TableField("slug")
    private String slug;

    /**
     * Force Show Flag
     */
    @TableField("force_show")
    private Boolean forceShow;

    /**
     * Is Carousel
     */
    @TableField("is_carousel")
    private Boolean isCarousel;

    /**
     * Publish Time
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;

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
