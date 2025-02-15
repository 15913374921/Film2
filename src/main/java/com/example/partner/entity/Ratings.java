package com.example.partner.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import cn.hutool.core.annotation.Alias;
import com.example.partner.common.LDTConfig;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;

/**
* <p>
* 
* </p>
*
* @author LZY
* @since 2024-11-28
*/
@Getter
@Setter
@ApiModel(value = "Ratings对象", description = "")
public class Ratings implements Serializable {

private static final long serialVersionUID = 1L;

    // 评分ID
    @ApiModelProperty("评分ID")
    @Alias("评分ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    // 电影或剧集ID
    @ApiModelProperty("电影或剧集ID")
    @Alias("电影或剧集ID")
    private Integer movitvId;

    // 用户ID
    @ApiModelProperty("用户ID")
    @Alias("用户ID")
    private Integer userId;

    // 评分
    @ApiModelProperty("评分")
    @Alias("评分")
    private Integer rating;

    // id类型
    @ApiModelProperty("id类型")
    @Alias("id类型")
    private String type;

    // 创建时间
    @ApiModelProperty("创建时间")
    @Alias("创建时间")
    @TableField(fill = FieldFill.INSERT)
    @JsonDeserialize(using = LDTConfig.CmzLdtDeSerializer.class)
    @JsonSerialize(using = LDTConfig.CmzLdtSerializer.class)
    private LocalDateTime createTime;

    // 更新时间
    @ApiModelProperty("更新时间")
    @Alias("更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonDeserialize(using = LDTConfig.CmzLdtDeSerializer.class)
    @JsonSerialize(using = LDTConfig.CmzLdtSerializer.class)
    private LocalDateTime updateTime;
}