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

    // 在项目启动成功后会执行该方法
    @Override
    public void run(ApplicationArguments args) throws Exception {

        try {
            RedisUtils.ping(); // redis的数据探测，初始化连接
            // 在项目启东时进行一次数据库查询，防止懒加载
            userMapper.select1();
            log.info("启动项目数据库连接成功");
            // 发送一次异步web请求，初始化web连接
            HttpUtil.get("http://localhost:9090/");
            log.info("启动项目Tomcat连接成功");
            new Thread(() -> {
                movieService.updatePopularMoviesCache();
                log.info("获取热门电影更新成功");
            }).start();

            new Thread(() -> {
                movieService.updateDAPopularMoviesCache();
                log.info("获取本日电影更新成功");
            }).start();

            new Thread(() -> {
                movieService.updateWEPopularMoviesCache();
                log.info("获取本周电影更新成功");
            }).start();

            new Thread(() -> {
                tvService.updatePopularTvCache();
                log.info("获取热门剧集更新成功");
            }).start();

            new Thread(() -> {
                tvService.updateDAPopularTvCache();
                log.info("获取本日剧集更新成功");
            }).start();

            new Thread(() -> {
                tvService.updateWEPopularTvCache();
                log.info("获取本周剧集更新成功");
            }).start();
            new Thread(() -> {
                movieService.cacheAllPopularMovies();
                movieService.getUpComingMedia();
                log.info("获取即将上映电影预告片更新成功");
            }).start();
            new Thread(() -> {
                tvService.cacheAllPopularTv();

                log.info("获取即将上映电视预告片更新成功");
            }).start();
            new Thread(() -> {
                tvService.getOnAirTvDetails();
                tvService.getTvMedia();
                log.info("获取正在播放电视更新成功");
            }).start();
        } catch (Exception e) {
            log.info("优化时间失败",e);
        }
    }
}
