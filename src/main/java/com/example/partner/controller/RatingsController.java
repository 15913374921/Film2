package com.example.partner.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.net.URLEncoder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.partner.common.Result;
import org.springframework.web.multipart.MultipartFile;
import com.example.partner.service.IRatingsService;
import com.example.partner.entity.Ratings;

import org.springframework.web.bind.annotation.RestController;

/**
* <p>
*  前端控制器
* </p>
*
* @author LZY
* @since 2024-11-28
*/
@RestController
@RequestMapping("/ratings")
public class RatingsController {

    @Resource
    private IRatingsService ratingsService;

    @PostMapping
    @SaCheckPermission("ratings.add")
    public Result save(@RequestBody Ratings ratings) {
        ratingsService.save(ratings);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("ratings.edit")
    public Result update(@RequestBody Ratings ratings) {
        QueryWrapper<Ratings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("movitv_id",ratings.getMovitvId());
        queryWrapper.eq("user_id",ratings.getUserId());
        ratingsService.update(ratings,queryWrapper);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ratings.delete")
    public Result delete(@PathVariable Integer id) {
        ratingsService.removeById(id);
        return Result.success();
    }

    @PostMapping("/del/batch")
    @SaCheckPermission("ratings.deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        ratingsService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping
    @SaCheckPermission("ratings.list")
    public Result findAll() {
        return Result.success(ratingsService.list());
    }

    @GetMapping("/{movietvId}/{userId}")
    @SaCheckPermission("ratings.list")
    public Result findOne(@PathVariable Integer movietvId,@PathVariable Integer userId) {
        QueryWrapper<Ratings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("movitv_id",movietvId);
        queryWrapper.eq("user_id",userId);
        Ratings ratings = ratingsService.getOne(queryWrapper);
        return Result.success(ratings);
    }

    @GetMapping("/recommend/page")
    public Result GetRecommend(@RequestParam Integer pageNum,
                               @RequestParam Integer pageSize,
                               @RequestParam Integer userId){
        return Result.success(ratingsService.getRecommend(pageNum,pageSize,userId));

    }

}
