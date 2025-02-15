package com.example.partner.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.partner.common.Constants;
import com.example.partner.common.enums.EmailCodeEnum;
import com.example.partner.domain.*;
import com.example.partner.entity.Admin;
import com.example.partner.entity.Permission;
import com.example.partner.entity.User;
import com.example.partner.exception.ServiceException;
import com.example.partner.mapper.UserMapper;
import com.example.partner.service.*;
import com.example.partner.utils.EmailUtils;
import com.example.partner.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements IUserService {

    private static final long TIME_IN_MS5 = 5 * 60 * 1000; // 表示5分钟的毫秒数

    @Autowired
    EmailUtils emailUtils;

    @Resource
    private UserMapper userMapper;

    @Resource
    private IAdminService adminService;

    @Autowired
    private UserPermissionService userpermissionService;

    @Autowired
    private PermissionService permissionService;

    @Override
    public LoginDTO login(UserRequest user) {
        try {
            // 如果是管理员登录
            if ("admin".equals(user.getType())) {
                Admin admin = adminService.getOne(new QueryWrapper<Admin>().eq("username", user.getUsername()));
                if (admin == null) {
                    throw new ServiceException("未找到用户");
                }
                if (!BCrypt.checkpw(user.getPassword(), admin.getPassword())) {
                    throw new ServiceException("用户名或密码错误");
                }

                // 登录管理员
                StpUtil.login(admin.getId());
                log.info("管理员已登录，ID: {}", admin.getId());
                StpUtil.getSession().set(Constants.LOGIN_USER_KEY, admin);

                String tokenValue = StpUtil.getTokenInfo().getTokenValue();
                log.info("生成的令牌: {}", tokenValue);

                // 管理员登录只返回admin和token
                return LoginDTO.adminLogin(admin, tokenValue);
            }

            // 普通用户登录
            User dbUser = getOne(new UpdateWrapper<User>().eq("username", user.getUsername())
                    .or().eq("email", user.getUsername()));
            if (dbUser == null) {
                throw new ServiceException("未找到用户");
            }
            if (!BCrypt.checkpw(user.getPassword(), dbUser.getPassword())) {
                throw new ServiceException("用户名或密码错误");
            }

            // 登录用户
            StpUtil.login(dbUser.getUid());
            log.info("用户已登录，UID: {}", dbUser.getUid());
            StpUtil.getSession().set(Constants.LOGIN_USER_KEY, dbUser);

            String tokenValue = StpUtil.getTokenInfo().getTokenValue();
            log.info("生成的令牌: {}", tokenValue);

            List<Permission> allPermissions = permissionService.getAllPermissions();
            List<String> allPermissionNames = allPermissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toList());

            // 获取用户当前拥有的权限
            List<String> userPermissions = userpermissionService.getPermissionsByUserId(dbUser.getId());
            StpUtil.getSession().set("userPermissions", userPermissions);
            log.info("用户当前权限: {}", userPermissions);

            // 计算用户缺少的权限
            List<String> missingPermissions = allPermissionNames.stream()
                    .filter(permission -> !userPermissions.contains(permission))
                    .collect(Collectors.toList());

            // 为用户添加缺少的权限
            if (!missingPermissions.isEmpty()) {
                userpermissionService.addPermissionsToUser(dbUser.getId(), missingPermissions);
                log.info("添加的权限: {}", missingPermissions);

                // 更新会话中的权限
                userPermissions.addAll(missingPermissions);
                StpUtil.getSession().set("userPermissions", userPermissions);
            }

            // 普通用户登录返回完整信息
            return LoginDTO.builder()
                    .user(dbUser)
                    .token(tokenValue)
                    .build();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("数据库异常", e);
        }
    }

    @Override
    public void register(UserRequest user) {
        // 校验邮箱
        String key = Constants.EMAIL_CODE + EmailCodeEnum.REGISTER.getValue() + user.getEmail();
        validateEmail(key,user.getEmailCode());
        try {
            User saveUser = new User();
            BeanUtils.copyProperties(user,saveUser);
            saveUser(saveUser);
        } catch (Exception e){
            throw new RuntimeException("数据库异常",e);
        }
    }

    @Override
    public void sendEmail(String email, String type) {
        String EmailPrefix = EmailCodeEnum.getValue(type);
        if(StrUtil.isBlank(EmailPrefix)) {
            throw new ServiceException("不支持的邮箱验证类型");
        }
        // 设redis缓存的key
        String key = Constants.EMAIL_CODE + EmailPrefix + email;
        Long expireTime = RedisUtils.getExpireTime(key);
        // 获取剩余时间大于3分钟，不允许发送
        if (expireTime != null && expireTime > 3 * 60) {
            throw new ServiceException("发送邮箱验证过于频繁");
        }
        Integer code = Integer.valueOf(RandomUtil.randomNumbers(6));
        log.info("本次验证码为：{}",code);
        String context = "<b>尊敬的用户，</b><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;您好，BS影评网提醒您本次的验证码是：<b>{}<b>，" +
                "有效期5分��。<br><br><br><b>BS影评网</b>";
        String html = StrUtil.format(context,code);
        // 检验邮箱是否注册
        User user = getOne(new QueryWrapper<User>().eq("email",email));
        if (EmailCodeEnum.REGISTER.equals(EmailCodeEnum.getEnum(type))) {
            if(user != null){
                throw new ServiceException("邮箱已注册");
            }
        } else if(EmailCodeEnum.RESETPASSWORD.equals(EmailCodeEnum.getEnum(type))) {
            if(user == null){
                throw new ServiceException("未找到用户");
            }
        }
        ThreadUtil.execAsync(() -> { // 多线程异步请求
            emailUtils.sendHtml("【BS影评网】邮箱验证",html,email);
            // 设置redis缓存
            RedisUtils.setCacheObject(key,code,TIME_IN_MS5, TimeUnit.MILLISECONDS);
        });
    }

    @Override
    public User passwordReset(UserRequest user) {
        String email = user.getEmail();
        User dbUser = getOne(new UpdateWrapper<User>().eq("email",email));
        if(dbUser == null){
            throw new ServiceException("未找到用户");
        }
        if(dbUser.getPassword().equals(user.getPassword())) {
            throw new ServiceException("不能和上一次密码一样");
        }
        // 校验邮箱验证码
        String key = Constants.EMAIL_CODE + EmailCodeEnum.RESETPASSWORD.getValue() + user.getEmail();
        validateEmail(key, user.getEmailCode());
        dbUser.setPassword(BCrypt.hashpw(user.getPassword()));
        try {
            updateById(dbUser);
        } catch (Exception e) {
            throw new RuntimeException("修改失败",e);
        }

        return dbUser;
    }

    private User saveUser(User user){
        User dbUser = getOne(new UpdateWrapper<User>().eq("username",user.getUsername()));
        if(dbUser != null){
            throw new ServiceException("用户已注册");
        }
        // 设置昵称
        if(StrUtil.isBlank(user.getName())){
            user.setName(Constants.USER_NAME_PREFIX + DateUtil.format(new Date(), Constants.DATE_RULE_YYYYMMDD));
        }
        // 设置密码加密
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        // 设置唯一标识
        user.setUid(IdUtil.fastSimpleUUID());
        try {
            save(user);
        } catch (Exception e) {
            throw new RuntimeException("注册失败", e);
        }

        return user;
    }

    private void validateEmail(String emailKey,String emailCode) {
        // 校验邮箱
        Integer code = RedisUtils.getCacheObject(emailKey);
        if(code == null) {
            throw new ServiceException("验证码失效");
        }
        if(!emailCode.equals(code.toString())) {
            throw new ServiceException("验证码错误");
        }
        RedisUtils.deleteObject(emailKey); // 清除缓存
    }

    @Override
    public Integer getIdByUid(String uid) {
        User user = userMapper.selectByUid(uid);
        return user != null ? user.getId() : null;
    }

    @Override
    public User getOne(Integer userId) {
        return getById(userId);
    }

    @Override
    public void logout(String uid) {
        StpUtil.logout(uid);
        log.info("用户{}退出成功",uid);
    }
}
