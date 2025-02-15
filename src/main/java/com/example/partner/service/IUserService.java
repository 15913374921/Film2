package com.example.partner.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.domain.LoginDTO;
import com.example.partner.domain.UserRequest;
import com.example.partner.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author LZY
 * @since 2024-09-19
 */
public interface IUserService extends IService<User> {

    LoginDTO login(UserRequest user);

    void register(UserRequest user);

    void sendEmail(String email, String type);

    User passwordReset(UserRequest user);

    Integer getIdByUid(String uid);

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    User getOne(Integer userId);

    void logout(String uid);
}
