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
 * Event Tag Association Table
 * </p>
 *
 * @author shuai
 * @since 2026-04-10
 */
@Getter
@Setter
@ToString
@TableName("event_tag")
public class EventTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary Key ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Event ID
     */
    @TableField("event_id")
    private String eventId;

    /**
     * Tag ID
     */
    @TableField("tag_id")
    private String tagId;

    /**
     * Creation Time
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * Logical Delete Flag
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;
}
