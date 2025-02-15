package com.example.partner.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("Permission")
@Data
public class Permission {
    private Integer id;
    private String name;
    private String description;

}
