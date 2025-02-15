package com.example.partner.common;



import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class MyFilter implements Filter {

    //时间窗口
    // 1秒内通过两个窗口
    private static volatile long startTime = System.currentTimeMillis();;
    private static final long windowTime = 1000L;

    private static final int door = 100;

    private static final AtomicInteger bear = new AtomicInteger(0); // 设置一个桶，用来存储请求次数
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        int count = bear.incrementAndGet(); // 来了一个请求加1
        if(count == 1) {  // 如果多个线程进来，会逐一增加，而不会是多个线程0 + 1
            startTime = System.currentTimeMillis();
        }
         // 发生了请求
        long now = System.currentTimeMillis();
        log.info("请求数量，count：{}",count);
        log.info("时间窗口，startTime：{}，count：{}",(now - startTime),count);
        if(now - startTime <= windowTime) {
            if(count > door) { // 超过了预值
                // 进行限流操作
                log.info("拦截成功,count：{}",count);
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                response.setStatus(HttpStatus.OK.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
                response.getWriter().print(JSONUtil.toJsonStr(Result.error("402", "接口请求失败")));
                return;
            }
        } else {
            startTime = System.currentTimeMillis();
            bear.set(1);
        }

        filterChain.doFilter(servletRequest,servletResponse);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
