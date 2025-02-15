package com.example.partner.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import cn.hutool.core.annotation.Alias;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.partner.common.LDTConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
* <p>
* 影视评论表
* </p>
*
* @author LZY
* @since 2024-12-05
*/
@Getter
@Setter
@ToString
@ApiModel(value = "Comments对象", description = "影视评论表")
public class Comments implements Serializable {

private static final long serialVersionUID = 1L;

    // 评论ID，自增主键
    @ApiModelProperty("评论ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 发表评论的用户ID
    @ApiModelProperty("用户ID")
    @TableField("user_id")
    private String userId;

    // 发表评论的用户名
    @ApiModelProperty("用户名")
    @TableField("username")
    private String username;

    // 用户头像URL
    @ApiModelProperty("用户头像")
    @TableField("avatar")
    private String avatar;

    // 电影/电视剧的ID
    @ApiModelProperty("媒体ID")
    @TableField("media_id")
    private Long mediaId;

    // 媒体类型：movie-电影，tv-电视剧
    @ApiModelProperty("媒体类型")
    @TableField("media_type")
    private String mediaType;

    // 评论内容
    @ApiModelProperty("评论内容")
    @TableField("content")
    private String content;

    // 评论创建时间
    @ApiModelProperty("评论时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonDeserialize(using = LDTConfig.CmzLdtDeSerializer.class)
    @JsonSerialize(using = LDTConfig.CmzLdtSerializer.class)
    private LocalDateTime createTime;

    // 评论获得的点赞数
    @ApiModelProperty("点赞数")
    @TableField("likes")
    private Integer likes;

    // 父评论ID，用于回复功能
    @ApiModelProperty("父评论ID")
    @TableField("parent_id")
    private Long parentId;

    // 被回复的用户名
    @ApiModelProperty("被回复的用户名")
    @TableField("reply_to")
    private String replyTo;

    // 被回复的用户ID
    @ApiModelProperty("被回复的用户ID")
    @TableField("reply_to_user_id")
    private String replyToUserId;

    // 回复列表，不映射到数据库
    @ApiModelProperty("回复列表")
    @TableField(exist = false)
    private List<Comments> replies;
}