package com.example.partner.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.partner.common.Result;
import com.example.partner.utils.DatabaseMigrationUtil;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/admin/migration")
public class MigrationController {

    @Resource
    private DatabaseMigrationUtil databaseMigrationUtil;

    @PostMapping("/migrate")
    @SaCheckPermission("admin")
    public Result migrateData() {
        try {
            databaseMigrationUtil.migrateData();
            return Result.success("数据迁移成功");
        } catch (Exception e) {
            return Result.error("数据迁移失败: " + e.getMessage());
        }
    }
} 