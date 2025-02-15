package com.example.partner.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.entity.Watchlist;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author LZY
 * @since 2024-11-13
 */
public interface IWatchlistService extends IService<Watchlist> {

    Page<Map<String, Object>> getWatchlistPage(Integer userId, Integer pageNum, Integer pageSize);

    void addMovieToWatchlist(Watchlist watchlist);
    
    void deleteFromWatchlist(Integer userId, Long movietvId,Integer page);

    void saveMedia(Watchlist watchlist);
}
