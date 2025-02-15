package com.example.partner.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import java.net.URLEncoder;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.common.Constants;
import com.example.partner.exception.ServiceException;
import com.example.partner.service.IUserService;
import com.example.partner.entity.User;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import com.example.partner.common.Result;
import org.springframework.web.multipart.MultipartFile;
import com.example.partner.service.ICommentsService;
import com.example.partner.entity.Comments;

import org.springframework.web.bind.annotation.RestController;

/**
* <p>
* 评论表 前端控制器
* </p>
*
* @author LZY
* @since 2024-12-05
*/
@RestController
@RequestMapping("/comments")
public class CommentsController {

    @Resource
    private ICommentsService commentsService;

    @Resource
    private IUserService userService;

    @PostMapping
    @SaCheckPermission("comments.add")
    public Result save(@RequestBody Comments comments) {
        // 获取当前会话的用户ID
        comments.setUserId(comments.getUserId());
        commentsService.addComment(comments);
        return Result.success();
    }

    @PutMapping
    @SaCheckPermission("comments.edit")
    public Result update(@RequestBody Comments comments) {
        commentsService.updateById(comments);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        commentsService.removeById(id);
        return Result.success();
    }

    @PostMapping("/del/batch")
    @SaCheckPermission("comments.deleteBatch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        commentsService.removeByIds(ids);
        return Result.success();
    }

    @GetMapping
    @SaCheckPermission("comments.list")
    public Result findAll() {
        return Result.success(commentsService.list());
    }

    @GetMapping("/{mediaType}/{mediaId}")
    public Result findOne(@PathVariable String mediaType, @PathVariable Long mediaId) {
        return Result.success(commentsService.getComments(mediaType, mediaId));
    }

    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "") String mediaType,
                           @RequestParam(defaultValue = "") String username,
                           @RequestParam Integer pageNum,
                           @RequestParam Integer pageSize) {
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<Comments>().orderByDesc("id");
        queryWrapper.like(StrUtil.isNotBlank(mediaType),"media_type",mediaType);
        queryWrapper.like(StrUtil.isNotBlank(username),"username",username);
        return Result.success(commentsService.page(new Page<>(pageNum, pageSize), queryWrapper));
    }

    @PostMapping("/{commentId}/like")
    @SaCheckLogin
    public Result likeComment(@PathVariable Long commentId, @RequestBody Map<String, Integer> requestBody) {
        Integer userId = requestBody.get("userId");
        try {
            String message = commentsService.likeComment(commentId, userId);
            return Result.success(message);
        } catch (ServiceException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reply")
    @SaCheckLogin
    public Result reply(@RequestBody Comments reply) {
        try {
            Comments savedReply = commentsService.addComment(reply);
            return Result.success(savedReply);
        } catch (Exception e) {
            return Result.error("回复失败：" + e.getMessage());
        }
    }

    @PostMapping("/exchange")
    @SaCheckLogin
    public Result exchangePoints(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer points = (Integer) params.get("points");
        
        try {
            // 获取用户当前积分
            User user = userService.getById(userId);
            if (user == null) {
                throw new ServiceException("用户不存在");
            }
            
            // 检查积分是否足够
            if (user.getScope() < points) {
                throw new ServiceException("积分不足");
            }
            
            // 扣除积分
            user.setScope(user.getScope() - points);
            userService.updateById(user);
            
            // 更新存储在Session中的用户信息
            StpUtil.getSession().set(Constants.LOGIN_USER_KEY, user);
            
            return Result.success("兑换成功");
        } catch (ServiceException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("兑换失败：" + e.getMessage());
        }
    }
}

