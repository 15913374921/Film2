package com.example.partner.common;

import cn.dev33.satoken.stp.StpInterface;
import com.example.partner.service.IUserService;
import com.example.partner.service.PermissionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomStpInterface implements StpInterface {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private IUserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String uid = loginId.toString();
        Integer userId = userService.getIdByUid(uid);
        List<String> permissions = permissionService.getPermissionsByUserId(userId);
        System.out.println("获取的用户权限: " + permissions);
        return permissions;
    }

    // 如果不需要角色管理，可以暂时注释掉这个方法
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return null; // 返回空列表表示没有角色
    }
}
