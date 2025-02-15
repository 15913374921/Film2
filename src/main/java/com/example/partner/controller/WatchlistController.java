package com.example.partner.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.net.URLEncoder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.exception.ServiceException;
import com.example.partner.service.IUserService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.partner.common.Result;
import org.springframework.web.multipart.MultipartFile;
import com.example.partner.service.IWatchlistService;
import com.example.partner.entity.Watchlist;

import org.springframework.web.bind.annotation.RestController;

/**
* <p>
*  前端控制器
* </p>
*
* @author LZY
* @since 2024-11-13
*/
@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    @Resource
    private IWatchlistService watchlistService;

    @Resource
    private IUserService iUserService;

    @PostMapping
    @SaCheckLogin
    @SaCheckPermission("watchlist.add")
    public Result save(@RequestBody Watchlist watchlist) {
        String loginId = (String) StpUtil.getLoginId();
        Integer userId = iUserService.getIdByUid(loginId);
        watchlist.setUserId(userId);

        
        watchlistService.saveMedia(watchlist);
        return Result.success();
    }


    @PutMapping
    @SaCheckPermission("watchlist.edit")
    public Result update(@RequestBody Watchlist watchlist) {
//        watchlistService.updateById(watchlist);
        return Result.success();
    }

    @DeleteMapping("/{Idtype}")
    @SaCheckPermission("watchlist.delete")
    public Result delete(@PathVariable Long Idtype,@RequestParam(required = false) Integer page) {
        String loginId = (String) StpUtil.getLoginId();
        Integer userId = iUserService.getIdByUid(loginId);
        watchlistService.deleteFromWatchlist(userId,Idtype,page);
        return Result.success();
    }

    @PostMapping("/del/batch")
    @SaCheckPermission("watchlist.deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        watchlistService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping
    @SaCheckPermission("watchlist.list")
    public Result findAll() {
        return Result.success(watchlistService.list());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("watchlist.list")
    public Result findOne(@PathVariable Integer id) {

        return Result.success(watchlistService.getById(id));
    }

    @GetMapping("/page")
    @SaCheckPermission("watchlist.list")
    public Result findPage(@RequestParam Integer pageNum,
                           @RequestParam Integer pageSize,
                           @RequestParam Integer userId) {
    
        // 获取当前用户的待看清单分页数据
        Page<Map<String, Object>> watchlistPage = watchlistService.getWatchlistPage(userId, pageNum, pageSize);
        return Result.success(watchlistPage);
    }

    /**
    * 导出接口
    */
    @GetMapping("/export")
    @SaCheckPermission("watchlist.export")
    public void export(HttpServletResponse response) throws Exception {
        // 从数据库查询出所有的数据
        List<Watchlist> list = watchlistService.list();
        // 在内存操作，写出到浏览器
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 一次性写出list内的对象到excel，使用默认样式，强制输出标题
        writer.write(list, true);

        // 设置浏览器响应的格式
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("Watchlist信息表", "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        ServletOutputStream out = response.getOutputStream();
        writer.flush(out, true);
        out.close();
        writer.close();

    }

    /**
    * excel 导入
    * @param file
    * @throws Exception
    */
    @PostMapping("/import")
    @SaCheckPermission("watchlist.import")
    public Result imp(MultipartFile file) throws Exception {
        InputStream inputStream = file.getInputStream();
        ExcelReader reader = ExcelUtil.getReader(inputStream);
        // 通过 javabean的方式读取Excel内的对象，但是要求表头必须是英文，跟javabean的属性要对应起来
        List<Watchlist> list = reader.readAll(Watchlist.class);

        watchlistService.saveBatch(list);
        return Result.success();
    }

}
