package com.example.partner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.common.ApiUrl;
import com.example.partner.domain.MovieDTO;
import com.example.partner.domain.TvDTO;
import com.example.partner.entity.Favoritelist;
import com.example.partner.exception.ServiceException;
import com.example.partner.mapper.FavoritelistMapper;
import com.example.partner.service.IFavoritelistService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author LZY
 * @since 2024-11-23
 */
@Service
public class FavoritelistServiceImpl extends ServiceImpl<FavoritelistMapper, Favoritelist> implements IFavoritelistService {

    @Resource
    FavoritelistMapper favoritelistMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final RestTemplate restTemplate;

    private final ApiUrl apiUrl;

    public FavoritelistServiceImpl(ApiUrl apiUrl) {
        this.restTemplate = createRestTemplateWithProxy();
        this.apiUrl = apiUrl;
    }

    private RestTemplate createRestTemplateWithProxy() {
        // 创建一个代理对象，指向 Clash 的本地地址和端口
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890)))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        OkHttpClient okHttpClient = builder.build();
        ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient);
        return new RestTemplate(requestFactory);
    }

    @Override
    public void saveMedia(Favoritelist favoritelist) {
        boolean exists = existsInWatchlist(favoritelist.getUserId(), favoritelist.getMovietvId());
        if (exists) {
            if("movie".equals(favoritelist.getType())){
                throw new ServiceException("该电影已存在收藏清单");
            } else if("tv".equals(favoritelist.getType())) {
                throw new ServiceException("该剧集已存在收藏清单");
            }
        }
        save(favoritelist);
        addMovieTofavoritelist(favoritelist);
    }

    public Page<Map<String, Object>> getfavoritelistPage(Integer userId, Integer pageNum, Integer pageSize) {
        // 定义 Redis 缓存键
        String cacheKey = "user:" + userId + ":favoritelist:page:" + pageNum + ":size:" + pageSize;

        // 尝试从 Redis 获取缓存数据
        Page<Map<String, Object>> cachedPage = (Page<Map<String, Object>>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedPage != null) {
            return cachedPage;
        }

        // 创建分页对象
        Page<Favoritelist> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Favoritelist> queryWrapper = new QueryWrapper<Favoritelist>()
                .eq("user_id", userId)
                .orderByDesc("created_at");

        // 分页查询用户的待看清单
        Page<Favoritelist> watchlistPage = favoritelistMapper.selectPage(page, queryWrapper);

        // 使用 CompletableFuture 异步获取电影和电视详情
        List<CompletableFuture<Map<String, Object>>> futures = watchlistPage.getRecords().stream()
                .map(watchlist -> CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> details = new HashMap<>();
                    String type = watchlist.getType();
                    Long id = watchlist.getMovietvId();

                    if ("movie".equals(type)) {
                        String urlWithParams = apiUrl.getMovieDetailsUrl(id);
                        MovieDTO movieResponse = restTemplate.getForObject(urlWithParams, MovieDTO.class);
                        details.put("movie", movieResponse);
                    } else if ("tv".equals(type)) {
                        String urlWithParams = apiUrl.getTvDetailsUrl(id);
                        TvDTO tvResponse = restTemplate.getForObject(urlWithParams, TvDTO.class);
                        details.put("tv", tvResponse);
                    }
                    return details;
                })).collect(Collectors.toList());

        // 等待所有异步任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 收集结果
        List<Map<String, Object>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        // 创建一个新的分页对象来存储结果
        Page<Map<String, Object>> resultPage = new Page<>(pageNum, pageSize);
        resultPage.setRecords(results);
        resultPage.setTotal(watchlistPage.getTotal());

        // 将结果存储到 Redis
        redisTemplate.opsForValue().set(cacheKey, resultPage, 10, TimeUnit.HOURS);

        return resultPage;
    }

    private boolean existsInWatchlist(Integer userId, Long movietvId) {
        QueryWrapper<Favoritelist> queryWrapper = new QueryWrapper<Favoritelist>()
                .eq("user_id", userId)
                .eq("movietv_id", movietvId);
        Long count = favoritelistMapper.selectCount(queryWrapper);
        return count > 0;
    }

    public void addMovieTofavoritelist(Favoritelist favoritelist) {

        // 立即使缓存失效
        invalidateCacheForUser(favoritelist.getUserId());

        // 异步更新缓存
        CompletableFuture.runAsync(() -> {
            updateCacheForUser(favoritelist.getUserId());
        });
    }

    private void updateCacheForUser(Integer userId) {
        // 这里可以选择更新第一页或所有页的数据
        getfavoritelistPage(userId, 1, 5);

    }

    private void invalidateCacheForUser(Integer userId) {
        String cacheKeyPattern = "user:" + userId + ":favoritelist:page:*";
        Set<String> keys = redisTemplate.keys(cacheKeyPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void deleteFromfavoritelist(Integer userId, Long movietvId, Integer affectedPage) {
        // 从数据库中删除记录
        QueryWrapper<Favoritelist> queryWrapper = new QueryWrapper<Favoritelist>()
                .eq("user_id", userId)
                .eq("movietv_id", movietvId);
        favoritelistMapper.delete(queryWrapper);

        // 立即使缓存失效
        invalidateCacheForUser(userId);

        // 异步更新缓存，仅更新受影响的页面
        if (affectedPage != null) {
            CompletableFuture.runAsync(() -> {
                updateCacheAfterDeletion(userId, affectedPage);
            });
        }
    }

    private void updateCacheAfterDeletion(Integer userId, int affectedPage) {
        // 仅更新受影响的页面
        getfavoritelistPage(userId, affectedPage, 5);

        // 如果删除操作可能影响到后续页面（例如，删除后数据向前移动），也更新后续页面
        int totalPages = calculateTotalPages(userId, 5);
        for (int i = affectedPage + 1; i <= totalPages; i++) {
            getfavoritelistPage(userId, i, 5);
        }
    }

    private int calculateTotalPages(Integer userId, int pageSize) {
        int totalRecords = Math.toIntExact(favoritelistMapper.selectCount(new QueryWrapper<Favoritelist>().eq("user_id", userId)));
        return (int) Math.ceil((double) totalRecords / pageSize);
    }
}
