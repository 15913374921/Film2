package com.example.partner.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.common.Result;
import com.example.partner.domain.LoginDTO;
import com.example.partner.domain.UserRequest;
import com.example.partner.entity.User;
import com.example.partner.service.IUserService;
//import com.example.partner.service.TmdbService;
import com.example.partner.service.TMDBService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


/*
swagger
http://localhost:9090/swagger-ui/index.html
* */

@Api(tags = "无权限接口列表")
@RestController
@Slf4j
public class WebController {

    @Resource
    IUserService userService;

    @Resource
    TMDBService tmdbService;

    @GetMapping(value = "/")
    @ApiOperation(value = "版本校验接口")
    public String version() {
        String ver = "partner-back-0.0.1-SNAPSHOT";  // 应用版本号
        Package aPackage = WebController.class.getPackage();
        String title = aPackage.getImplementationTitle();
        String version = aPackage.getImplementationVersion();
        if (title != null && version != null) {
            ver = String.join("-", title, version);
        }
        // 在项目启东时进行一次数据库查询，防止懒加载
        userService.getById(1);
        log.info("启动项目数据库连接成功");
        return ver;
    }

    @ApiOperation(value = "用户登录接口")
    @PostMapping("/login")
    public Result login(@RequestBody UserRequest user) {
        System.out.println(user);
        LoginDTO res = userService.login(user);
        System.out.println("登录成功，当前用户权限: " + StpUtil.getSession().get("userPermissions"));

        return Result.success(res);
    }

    @ApiOperation(value = "用户退出接口")
    @GetMapping("/logout/{uid}")
    public Result logout(@PathVariable String uid) {
        userService.logout(uid);
        return Result.success();
    }

    @ApiOperation(value = "用户注册接口")
    @PostMapping("/register")
    public Result register(@RequestBody UserRequest user) {
        userService.register(user);
        return Result.success();
    }

    @ApiOperation(value = "邮箱接口")
    @GetMapping("/email")
    public Result sendEmail(@RequestParam String email, @RequestParam String type) {
        userService.sendEmail(email,type);
        return Result.success();
    }

    @ApiOperation(value = "密码重置接口")
    @PostMapping("/password/reset")
    public Result passwordReset(@RequestBody UserRequest user) {
        User res = userService.passwordReset(user);
        return Result.success(res);
    }


    @ApiOperation(value = "获取电影或剧集详情")
    @GetMapping("/movieTv/{id}/detail")
    public Result getDetail(@PathVariable Long id, @RequestParam String type) {
        System.out.println(id + type);
        ResponseEntity<String> response = tmdbService.getDetail(id, type);
        return Result.success(response);
    }

    @ApiOperation(value = "获取电影或剧集观看平台信息")
    @GetMapping("/movieTv/watch/{id}")
    public Result getWatch(@PathVariable Long id, @RequestParam String type) {
        ResponseEntity<String> response = tmdbService.getWatch(id, type);
        return Result.success(response);
    }

    @ApiOperation(value = "获取电影或剧集演员信息")
    @GetMapping("/movieTv/credits/{id}")
    public Result getCredits(@PathVariable Long id,@RequestParam String type) {
        ResponseEntity<String> response = tmdbService.getCredits(id, type);
        return Result.success(response);
    }

    @ApiOperation(value = "获取电影或剧集相似")
    @GetMapping("/movieTv/similar/{id}")
    public Result getSimilar(@PathVariable Long id,@RequestParam String type) {
        ResponseEntity<String> response = tmdbService.getSimilar(id, type);
        return Result.success(response);
    }

    @ApiOperation(value = "获取电影或剧集相似")
    @GetMapping("/SearchMovieTv")
    public Result getSearchMovieTv(@RequestParam Integer pageNum,
                                   @RequestParam Integer pageSize,
                                   @RequestParam String q,
                                   @RequestParam String type) {
        try {
            Page<JsonNode> page = tmdbService.getSearchMovieTv(pageNum, type, pageSize, q);
            return Result.success(page);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 处理错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("获取电影或剧集相似时发生错误: " + e.getMessage(), e);
        }
    }
//
//    @ApiOperation(value = "用户退出登录接口")
//    @GetMapping("/logout/{uid}")
//    public Result logout(@PathVariable String uid) {
//        userService.logout(uid);
//        return Result.success();
//    }
//
//    @ApiOperation(value = "用户注册接口")
//    @PostMapping("/register")
//    public Result register(@RequestBody UserRequest user) {
//        userService.register(user);
//        return Result.success();
//    }
//
//    @ApiOperation(value = "邮箱验证接口")
//    @GetMapping("/email")
//    public Result sendEmail(@RequestParam String email, @RequestParam String type) {  //  ?email=xxx&type=xxx
//        long start = System.currentTimeMillis();
//        userService.sendEmail(email, type);
//        log.info("发送邮件花费时间：{}", System.currentTimeMillis() - start);
//        return Result.success();
//    }
//
//    @ApiOperation(value = "密码重置接口")
//    @PostMapping("/password/reset")
//    public Result passwordReset(@RequestBody UserRequest userRequest) {
//        String newPass = userService.passwordReset(userRequest);
//        return Result.success(newPass);
//    }
//
//    // 修改密码
//    @PostMapping("/password/change")
//    public Result passwordChange(@RequestBody UserRequest userRequest) {
//        userService.passwordChange(userRequest);
//        return Result.success();
//    }
//
//    // 更新个人信息
//    @PutMapping("/updateUser")
//    public Result updateUser(@RequestBody User user) {
//        Object loginId = StpUtil.getLoginId();
//        if (!loginId.equals(user.getUid())) {
//            Result.error("无权限");
//        }
//        userService.updateById(user);
//        return Result.success(user);
//    }

}
