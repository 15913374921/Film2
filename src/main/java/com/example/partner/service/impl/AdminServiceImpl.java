package com.example.partner.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.partner.common.Constants;
import com.example.partner.entity.Admin;
import com.example.partner.entity.User;
import com.example.partner.exception.ServiceException;
import com.example.partner.mapper.AdminMapper;
import com.example.partner.service.IAdminService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements IAdminService {
    @Override
    public void saveUser(Admin admin) {
        Admin dbadmin = getOne(new UpdateWrapper<Admin>().eq("username",admin.getUsername()));
        if(dbadmin != null){
            throw new ServiceException("用户已注册");
        }
        // 设置昵称
        if(StrUtil.isBlank(admin.getName())){
            admin.setName(Constants.USER_NAME_PREFIX + DateUtil.format(new Date(), Constants.DATE_RULE_YYYYMMDD));
        }
        String password = "admin";
        // 设置密码加密
        admin.setPassword(BCrypt.hashpw(admin.getPassword()));
        try {
            save(admin);
        } catch (Exception e) {
            throw new RuntimeException("注册失败", e);
        }
    }
}