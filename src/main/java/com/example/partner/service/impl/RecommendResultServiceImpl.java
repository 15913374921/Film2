package com.example.partner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.partner.entity.RecommendResult;
import com.example.partner.entity.Ratings;
import com.example.partner.mapper.RecommendResultMapper;
import com.example.partner.mapper.RatingsMapper;
import com.example.partner.service.IRecommendResultService;
import com.example.partner.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.col;

@Slf4j
@Service
public class RecommendResultServiceImpl extends ServiceImpl<RecommendResultMapper, RecommendResult> implements IRecommendResultService {

    private static final String LAST_UPDATE_KEY = "recommend:last_update_time";
    private static final String PROCESSING_KEY = "recommend:processing";
    private static final int MIN_RATING_CHANGES = 10; // 最小评分变化数触发推荐
    private static final int MIN_UPDATE_INTERVAL = 60; // 两次更新的最小间隔（分钟）

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String jdbcUsername;

    @Value("${spring.datasource.password}")
    private String jdbcPassword;

    @Resource
    private SparkSession sparkSession;

    @Resource
    private RatingsMapper ratingsMapper;

    @Override
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    @Transactional
    public void executeOfflineRecommend() {
        executeOfflineRecommendInternal(false);
    }

    /**
     * 手动触发离线推荐（管理员使用）
     * @param force 是否强制执行，忽略时间间隔限制
     * @return 是否成功触发
     */
    @Override
    public boolean triggerOfflineRecommend(boolean force) {
        return executeOfflineRecommendInternal(force);
    }

    /**
     * 评分更新时触发检查
     * @param userId 用户ID
     */
    @Override
    public void checkAndTriggerRecommend(Integer userId) {
        // 获取最近的评分更新次数
        String ratingChangesKey = "recommend:rating_changes";
        Long changes = RedisUtils.increment(ratingChangesKey, 1L);
        RedisUtils.expire(ratingChangesKey, 1, TimeUnit.HOURS);

        // 如果评分变化达到阈值，触发推荐
        if (changes >= MIN_RATING_CHANGES) {
            RedisUtils.deleteObject(ratingChangesKey);
            executeOfflineRecommendInternal(false);
        }
    }

    private boolean executeOfflineRecommendInternal(boolean force) {
        // 检查是否正在处理
        if (RedisUtils.getCacheObject(PROCESSING_KEY) != null) {
            log.info("离线推荐正在进行中，跳过本次执行");
            return false;
        }

        // 检查上次更新时间
        if (!force) {
            LocalDateTime lastUpdate = RedisUtils.getCacheObject(LAST_UPDATE_KEY);
            if (lastUpdate != null && 
                lastUpdate.plusMinutes(MIN_UPDATE_INTERVAL).isAfter(LocalDateTime.now())) {
                log.info("距离上次更新时间未超过{}分钟，跳过本次执行", MIN_UPDATE_INTERVAL);
                return false;
            }
        }

        try {
            // 标记开始处理
            RedisUtils.setCacheObject(PROCESSING_KEY, true, 1, TimeUnit.HOURS);
            
            log.info("开始执行离线推荐计算...");
            
            // 加载评分数据
            Dataset<Row> ratingsData = sparkSession.read()
                    .format("jdbc")
                    .option("url", jdbcUrl)
                    .option("dbtable", "ratings")
                    .option("user", jdbcUsername)
                    .option("password", jdbcPassword)
                    .load();

            // 分别处理电影和电视剧
            processMediaTypeRecommendations(ratingsData, "movie");
            processMediaTypeRecommendations(ratingsData, "tv");

            // 更新最后处理时间
            RedisUtils.setCacheObject(LAST_UPDATE_KEY, LocalDateTime.now(), 24, TimeUnit.HOURS);
            
            log.info("离线推荐计算完成");
            return true;
        } catch (Exception e) {
            log.error("离线推荐计算失败", e);
            return false;
        } finally {
            // 清除处理中标记
            RedisUtils.deleteObject(PROCESSING_KEY);
        }
    }

    private void processMediaTypeRecommendations(Dataset<Row> ratingsData, String mediaType) {
        // 过滤指定媒体类型的数据
        Dataset<Row> typeData = ratingsData.filter(col("type").equalTo(mediaType));

        // 训练ALS模型
        ALS als = new ALS()
                .setMaxIter(10)
                .setRegParam(0.1)
                .setRank(10)
                .setAlpha(1.0)
                .setUserCol("user_id")
                .setItemCol("movitv_id")
                .setRatingCol("rating");

        ALSModel model = als.fit(typeData);

        // 获取所有用户
        List<Integer> userIds = ratingsMapper.selectList(null)
                .stream()
                .map(Ratings::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 为每个用户生成推荐
        for (Integer userId : userIds) {
            try {
                generateUserRecommendations(model, typeData, userId, mediaType);
            } catch (Exception e) {
                log.error("为用户{}生成{}推荐时发生错误", userId, mediaType, e);
            }
        }
    }

    private void generateUserRecommendations(ALSModel model, Dataset<Row> typeData, Integer userId, String mediaType) {
        // 获取用户未评分的媒体
        Dataset<Row> userRated = typeData.filter(col("user_id").equalTo(userId))
                .select("movitv_id");
        
        Dataset<Row> allItems = typeData.select("movitv_id").distinct();
        Dataset<Row> candidateItems = allItems.except(userRated);

        // 生成用户-物品对
        Dataset<Row> userItems = candidateItems.selectExpr(userId + " as user_id", "movitv_id");

        // 预测评分
        Dataset<Row> predictions = model.transform(userItems)
                .orderBy(col("prediction").desc())
                .limit(1); // 为每个用户推荐1个项目，因为mediaIds是Long类型

        // 收集推荐结果
        List<Row> recommendRows = predictions.collectAsList();
        
        if (!recommendRows.isEmpty()) {
            Row recommendRow = recommendRows.get(0);
            Long mediaId = recommendRow.getAs("movitv_id");
            double score = recommendRow.getAs("prediction");

            // 保存或更新推荐结果
            QueryWrapper<RecommendResult> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId)
                    .eq("media_type", mediaType);
            
            RecommendResult recommendResult = getOne(queryWrapper);
            if (recommendResult == null) {
                recommendResult = new RecommendResult();
                recommendResult.setUserId(userId);
                recommendResult.setMediaType(mediaType);
            }
            
            recommendResult.setMediaIds(mediaId);
            recommendResult.setScores(String.format("%.2f", score));
            recommendResult.setUpdateTime(LocalDateTime.now());
            recommendResult.setIsRead(false);
            
            saveOrUpdate(recommendResult);
            log.info("推薦計算完畢");
        }
    }

    @Override
    public RecommendResult getUserRecommendations(String userId, String mediaType) {
        QueryWrapper<RecommendResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", Integer.parseInt(userId))
                .eq("media_type", mediaType);
        
        RecommendResult result = getOne(queryWrapper);
        if (result != null) {
            // 标记为已读
            result.setIsRead(true);
            updateById(result);
        }
        return result;
    }
} 