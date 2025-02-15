package com.example.partner.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.entity.Favoritelist;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author LZY
 * @since 2024-11-23
 */
public interface IFavoritelistService extends IService<Favoritelist> {

    void saveMedia(Favoritelist favoritelist);

    void deleteFromfavoritelist(Integer userId, Long idtype, Integer page);

    Page<Map<String, Object>> getfavoritelistPage(Integer userId, Integer pageNum, Integer pageSize);
}
