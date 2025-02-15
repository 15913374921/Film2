package com.example.partner.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recommend_result")
public class RecommendResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Integer userId;
    
    private Long mediaIds;
    
    private String mediaType;
    
    private String scores;
    
    private LocalDateTime updateTime;
    
    private Boolean isRead;
} 