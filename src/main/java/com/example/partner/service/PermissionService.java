package com.example.partner.service;

import com.example.partner.entity.Permission;
import com.example.partner.mapper.PermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {
    @Autowired
    private PermissionMapper permissionMapper;

    public List<Permission> getAllPermissions() {
        return permissionMapper.findPermissions();
    }

    public List<String> getPermissionsByUserId(Integer userId) {
        return permissionMapper.getPermissionsByUserId(userId);
    }

}
