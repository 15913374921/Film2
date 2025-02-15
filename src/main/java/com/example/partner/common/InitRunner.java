package com.example.partner.common;

import cn.hutool.http.HttpUtil;
import com.example.partner.mapper.UserMapper;
import com.example.partner.service.MovieService;
import com.example.partner.service.TvService;
import com.example.partner.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class InitRunner implements ApplicationRunner {

    @Resource
    UserMapper userMapper;

    @Resource
    private MovieService movieService;

    @Resource
    private TvService tvService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 测试Redis连接
            try {
                RedisUtils.ping();
                log.info("Redis连接成功");
            } catch (Exception e) {
                log.error("Redis连接失败", e);
            }

            // 测试数据库连接
            try {
                userMapper.select1();
                log.info("数据库连接成功");
            } catch (Exception e) {
                log.error("数据库连接失败", e);
            }

            // 测试Web服务
            try {
                HttpUtil.get("http://localhost:9090/");
                log.info("Web服务启动成功");
            } catch (Exception e) {
                log.error("Web服务启动失败", e);
            }

            // 初始化电影和电视剧数据
            initializeMediaData();
        } catch (Exception e) {
            log.error("初始化过程中发生错误", e);
        }
    }

    private void initializeMediaData() {
        try {
            new Thread(() -> {
                try {
                    movieService.updatePopularMoviesCache();
                    log.info("获取热门电影更新成功");
                } catch (Exception e) {
                    log.error("获取热门电影更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    movieService.updateDAPopularMoviesCache();
                    log.info("获取本日电影更新成功");
                } catch (Exception e) {
                    log.error("获取本日电影更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    movieService.updateWEPopularMoviesCache();
                    log.info("获取本周电影更新成功");
                } catch (Exception e) {
                    log.error("获取本周电影更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    tvService.updatePopularTvCache();
                    log.info("获取热门剧集更新成功");
                } catch (Exception e) {
                    log.error("获取热门剧集更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    tvService.updateDAPopularTvCache();
                    log.info("获取本日剧集更新成功");
                } catch (Exception e) {
                    log.error("获取本日剧集更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    tvService.updateWEPopularTvCache();
                    log.info("获取本周剧集更新成功");
                } catch (Exception e) {
                    log.error("获取本周剧集更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    movieService.cacheAllPopularMovies();
                    movieService.getUpComingMedia();
                    log.info("获取即将上映电影预告片更新成功");
                } catch (Exception e) {
                    log.error("获取即将上映电影预告片更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    tvService.cacheAllPopularTv();
                    log.info("获取即将上映电视预告片更新成功");
                } catch (Exception e) {
                    log.error("获取即将上映电视预告片更新失败", e);
                }
            }).start();

            new Thread(() -> {
                try {
                    tvService.getOnAirTvDetails();
                    tvService.getTvMedia();
                    log.info("获取正在播放电视更新成功");
                } catch (Exception e) {
                    log.error("获取正在播放电视更新失败", e);
                }
            }).start();
        } catch (Exception e) {
            log.error("媒体数据初始化失败", e);
        }
    }
}
