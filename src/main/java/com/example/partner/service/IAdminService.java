package com.example.partner.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.partner.entity.Admin;

public interface IAdminService extends IService<Admin> {
    void saveUser(Admin admin);
}