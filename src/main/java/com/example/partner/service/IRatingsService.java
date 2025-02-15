package com.example.partner.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.entity.Ratings;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author LZY
 * @since 2024-11-28
 */
public interface IRatingsService extends IService<Ratings> {

    Page getRecommend(Integer pageNum, Integer pageSize,Integer userId);

}
