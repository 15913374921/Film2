package com.example.partner.service;

import com.example.partner.mapper.PermissionMapper;
import com.example.partner.mapper.UserPermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserPermissionService {

    @Autowired
    private UserPermissionMapper userPermissionMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    public List<String> getPermissionsByUserId(Integer userId) {
        return userPermissionMapper.findPermissionsByUserId(userId);
    }

    public void addPermissionsToUser(Integer id, List<String> missingPermissions) {
        List<Integer> permissionIds = permissionMapper.findPermissionIdsByNames(missingPermissions);
        userPermissionMapper.addPermissionsToUser(id, permissionIds);
    }
}
