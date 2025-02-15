package com.example.partner.controller.admin;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.common.Result;
import com.example.partner.domain.LoginDTO;
import com.example.partner.entity.Admin;
import com.example.partner.service.IAdminService;
import com.example.partner.service.IUserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private IUserService userService;

    @Resource
    private IAdminService adminService;

    @GetMapping("/page")
    @SaCheckLogin
    public Result findPage(@RequestParam(defaultValue = "") String name,
                          @RequestParam(defaultValue = "") String username,
                          @RequestParam Integer pageNum,
                          @RequestParam Integer pageSize) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<Admin>().orderByDesc("id");
        queryWrapper.like(!"".equals(name), "name", name);
        queryWrapper.like(!"".equals(username), "username", username);
        return Result.success(adminService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @GetMapping("/{id}")
    @SaCheckLogin
    public Result findOne(@PathVariable Integer id) {
        return Result.success(adminService.getById(id));
    }

    @PostMapping
    @SaCheckLogin
    public Result save(@RequestBody Admin admin) {
        System.out.println(admin);
        adminService.saveUser(admin);
        return Result.success();
    }

    @PutMapping
    @SaCheckLogin
    public Result update(@RequestBody Admin admin) {
        adminService.updateById(admin);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @SaCheckLogin
    public Result delete(@PathVariable Integer id) {
        adminService.removeById(id);
        return Result.success();
    }

    @PostMapping("/del/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        adminService.removeByIds(ids);
        return Result.success();
    }

    @PutMapping("/{id}/status/{status}")
    @SaCheckLogin
    public Result updateStatus(@PathVariable Integer id, @PathVariable Integer status) {
        Admin admin = adminService.getById(id);
        if (admin == null) {
            return Result.error("管理员不存在");
        }
        admin.setStatus(status);
        adminService.updateById(admin);
        return Result.success();
    }

    /**
     * 导出接口
     */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws Exception {
        // 从数据库查询出所有的数据
        List<Admin> list = adminService.list();
        // 在内存操作，写出到浏览器
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 一次性写出list内的对象到excel，使用默认样式，强制输出标题
        writer.write(list, true);

        // 设置浏览器响应的格式
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        String fileName = URLEncoder.encode("管理员信息表", "UTF-8");
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
    public Result imp(MultipartFile file) throws Exception {
        InputStream inputStream = file.getInputStream();
        ExcelReader reader = ExcelUtil.getReader(inputStream);
        // 通过 javabean的方式读取Excel内的对象，但是要求表头必须是英文，跟javabean的属性要对应起来
        List<Admin> list = reader.readAll(Admin.class);

        for(Admin admin : list){
            adminService.saveUser(admin);
        }
        return Result.success();
    }
} 