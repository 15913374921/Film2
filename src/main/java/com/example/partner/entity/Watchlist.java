package com.example.partner.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import cn.hutool.core.annotation.Alias;

import com.baomidou.mybatisplus.annotation.*;
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
* @since 2024-11-13
*/
@Getter
@Setter
@ApiModel(value = "Watchlist对象", description = "")
public class Watchlist implements Serializable {

private static final long serialVersionUID = 1L;

    // 主键
    @ApiModelProperty("主键")
    @Alias("主键")
    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;

    // 用户唯一标识
    @ApiModelProperty("用户唯一标识")
    @Alias("用户唯一标识")
    private Integer userId;

    // 电影或电视id
    @ApiModelProperty("电影或电视id")
    @Alias("电影id")
    private Long movietvId;

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
    private LocalDateTime createdAt;
}