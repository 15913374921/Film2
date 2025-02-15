package com.example.partner.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@SuppressWarnings(value = {"unchecked", "rawtypes"})
@Slf4j
public class RedisUtils {

    private static RedisTemplate<String,Object> staticRedisTemplate;

    private final RedisTemplate<String,Object> redisTemplate;

    public RedisUtils(RedisTemplate<String,Object>  redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 在springboots启动后执行
    @PostConstruct
    public void initRedis() {
        // 初始化设置 静态staticRedisTemplate对象，方便后续操作数据
        staticRedisTemplate = redisTemplate;
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public static <T> void setCacheObject(final String key, final T value) {
        staticRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public static <T> void setCacheObject(final String key, final T value, final long timeout, final TimeUnit timeUnit) {
        staticRedisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
      * 判断set中是否存在value
      * @param key 键
      * @param value 值
      * @return true 存在 false不存在
      */
    public static boolean sHasKey(String key, Object value) {
        try {
            return staticRedisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            log.error("Redis sHasKey error: ", e);
            return false;
        }
    }
            /**
             * 获得缓存的基本对象。
             *
             * @param key 缓存键值
             * @return 缓存键值对应的数据
             */
    public static <T> T getCacheObject(final String key) {
        return (T) staticRedisTemplate.opsForValue().get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public static boolean deleteObject(final String key) {
        return Boolean.TRUE.equals(staticRedisTemplate.delete(key));
    }

    /**
     * 获取单个key的过期时间
     * @param key
     * @return
     */
    public static Long getExpireTime(final String key) {
        return staticRedisTemplate.getExpire(key);
    }

    /**
     * 发送ping命令
     * redis 返回pong
     */
    public static void ping() {
        String res = staticRedisTemplate.execute(RedisConnectionCommands::ping);
        log.info("Redis ping ==== {}",res);
    }

    /**
     * 递增
     * @param key 键
     * @param delta 要增加几
     * @return 增加后的值
     */
    public static Long increment(String key, long delta) {
        return staticRedisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 设置过期时间
     * @param key 键
     * @param timeout 时间
     * @param unit 时间单位
     * @return true成功 false失败
     */
    public static Boolean expire(String key, long timeout, TimeUnit unit) {
        return staticRedisTemplate.expire(key, timeout, unit);
    }

}
