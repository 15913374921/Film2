package com.example.partner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.common.ApiUrl;
import com.example.partner.domain.MediaWrapper;
import com.example.partner.domain.MovieDTO;
import com.example.partner.domain.TvDTO;
import com.example.partner.entity.Ratings;
import com.example.partner.mapper.RatingsMapper;
import com.example.partner.service.IRatingsService;
import com.example.partner.service.IRecommendResultService;

import com.example.partner.utils.RedisUtils;
import okhttp3.OkHttpClient;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.explode;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author LZY
 * @since 2024-11-28
 */
@Service
public class RatingsServiceImpl extends ServiceImpl<RatingsMapper, Ratings> implements IRatingsService {

    @Autowired
    private IRecommendResultService recommendResultService;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String jdbcUsername;

    @Value("${spring.datasource.password}")
    private String jdbcPassword;

    @Resource
    private SparkSession sparkSession;

     private final RestTemplate restTemplate;

    private final ApiUrl apiUrl;

    public RatingsServiceImpl(ApiUrl apiUrl) {
        this.restTemplate = createRestTemplateWithProxy();
        this.apiUrl = apiUrl;
    }

    private RestTemplate createRestTemplateWithProxy() {
        // 创建一个代理对象，指 Clash 的本地地址和端口
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890)))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        OkHttpClient okHttpClient = builder.build();
        ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory(okHttpClient);
        return new RestTemplate(requestFactory);
    }

    // spark推荐算法


    @Override
    public Page getRecommend(Integer pageNum, Integer pageSize, Integer userId) {
        // 首先检查Redis中是否存在推荐结果
        String recommendKey = "user:recommend:" + userId;
        List<Object> cachedRecommendations = RedisUtils.getCacheObject(recommendKey);

        if (cachedRecommendations != null && !cachedRecommendations.isEmpty()) {
            // 如果Redis中存在推荐结果，直接使用缓存数据
            Page<Object> page = new Page<>(pageNum, pageSize);
            page.setTotal(cachedRecommendations.size());
            int start = (pageNum - 1) * pageSize;
            int end = Math.min(start + pageSize, cachedRecommendations.size());
            page.setRecords(cachedRecommendations.subList(start, end));
            return page;
        }

        // Redis中没有，执行推荐算法
        Dataset<Row> ratingsData = sparkSession.read()
                .format("jdbc")
                .option("url", jdbcUrl)
                .option("dbtable", "ratings")
                .option("user", jdbcUsername)
                .option("password", jdbcPassword)
                .load();

        // 获取用户所有已评分的电影
        Dataset<Row> userRatedMovies = ratingsData
                .filter(col("user_id").equalTo(userId))
                .select("movitv_id");

        // 获取候选电影的多层次策略
        Dataset<Row> candidateMovies = getCandidateMovies(ratingsData, userId, userRatedMovies);

        ALS als = new ALS()
                .setMaxIter(10)
                .setRegParam(0.1)
                .setRank(10)
                .setAlpha(1.0)
                .setUserCol("user_id")
                .setItemCol("movitv_id")
                .setRatingCol("rating");

        // 训练模型
        ALSModel model = als.fit(ratingsData);

        // 为指定用户生成推荐
        Dataset<Row> userSubset = sparkSession.createDataFrame(
                Collections.singletonList(
                    RowFactory.create(userId)
                ),
                DataTypes.createStructType(new StructField[]{
                    DataTypes.createStructField("user_id", DataTypes.IntegerType, false)
                })
        );

        // 交叉连接用户和候选电影，生成待预测的用户-电影对
        Dataset<Row> userMoviePairs = userSubset.crossJoin(candidateMovies);

        // 使用模型预测评分
        Dataset<Row> predictions = model.transform(userMoviePairs);

        // 获取预测评分最高的推荐结果
        Dataset<Row> recommendations = predictions
                .orderBy(col("prediction").desc())
                .limit(pageSize * 2);

        // 转换为List
        List<Row> recommendationsList = recommendations.collectAsList();

        // 创建分页对象
        Page<Object> page = new Page<>(pageNum, pageSize);

        // 获取推荐的电影ID列表
        List<Long> movieTvIds = recommendationsList.stream()
                .map(row -> {
                    Object movieIdObj = row.getAs("movitv_id");
                    return movieIdObj instanceof Long ? (Long) movieIdObj :
                           Long.valueOf(movieIdObj.toString());
                })
                .collect(Collectors.toList());

        List<Object> resultList = new ArrayList<>();

        for (Long movieTvId : movieTvIds) {
            Ratings ratings = getRatingsByMovieTvId(movieTvId);
            String type = ratings.getType();

            // 先从Redis中查找电影/剧集详情
            String detailKey = type + ":detail:" + movieTvId;
            Object cachedDetail = RedisUtils.getCacheObject(detailKey);

            if (cachedDetail != null) {
                resultList.add(new MediaWrapper(cachedDetail, type));
                continue;
            }

            // Redis中没有，调用API获取
            try {
                if ("movie".equals(type)) {
                    String urlWithParams = apiUrl.getMovieDetailsUrl(movieTvId);
                    MovieDTO movieResponse = restTemplate.getForObject(urlWithParams, MovieDTO.class);
                    if (movieResponse != null) {
                        // 存储电影详情到Redis
                        RedisUtils.setCacheObject(detailKey, movieResponse, 24, TimeUnit.HOURS);
                        resultList.add(new MediaWrapper(movieResponse, type));
                    }
                } else if ("tv".equals(type)) {
                    String urlWithParams = apiUrl.getTvDetailsUrl(movieTvId);
                    TvDTO tvResponse = restTemplate.getForObject(urlWithParams, TvDTO.class);
                    if (tvResponse != null) {
                        // 存储剧集详情到Redis
                        RedisUtils.setCacheObject(detailKey, tvResponse, 3, TimeUnit.HOURS);
                        resultList.add(new MediaWrapper(tvResponse, type));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // 在获取完所有推荐结果后，存入Redis
        RedisUtils.setCacheObject(recommendKey, resultList, 3, TimeUnit.HOURS);

        // 创建分页对象
        page = new Page<>(pageNum, pageSize);
        // 设置总记录数
        page.setTotal(resultList.size());
        // 计算当前页的数据
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, resultList.size());
        // 设置当前页的记录
        page.setRecords(resultList.subList(start, end));
        
        return page;
    }

    /**
     * 获取候选电影的多层次策略
     */
    private Dataset<Row> getCandidateMovies(Dataset<Row> ratingsData, Integer userId, Dataset<Row> userRatedMovies) {
        // 策略1：基于用户高分电影的相似用户推荐
        Dataset<Row> highRatedCandidates = getHighRatedBasedCandidates(ratingsData, userId, userRatedMovies);
        
        // 如果有基于相同高分电影的推荐，直接返回
        if (highRatedCandidates.count() > 0) {
            return highRatedCandidates;
        }

        // 策略2：获取所有用户评分高的电影
        Dataset<Row> highRatedMovies = getAllUsersHighRatedMovies(ratingsData, userRatedMovies);
        
        if (highRatedMovies.count() > 0) {
            return highRatedMovies;
        }

        // 策略3：如果前两种策略都没有结果，返回评分最多的电影
        return ratingsData.groupBy("movitv_id")
                .agg(
                    functions.count("rating").as("rating_count"),
                    functions.avg("rating").as("avg_rating")
                )
                .orderBy(
                    col("rating_count").desc(),
                    col("avg_rating").desc()
                )
                .select("movitv_id")
                .limit(10);
    }

    /**
     * 策略1：基于用户高分电影的相似用户推荐
     */
    private Dataset<Row> getHighRatedBasedCandidates(Dataset<Row> ratingsData, Integer userId, Dataset<Row> userRatedMovies) {
        // 获取用户高分评价的电影（评分大于7分）
        Dataset<Row> userHighRatedMovies = ratingsData
                .filter(col("user_id").equalTo(userId)
                        .and(col("rating").geq(7.0)))
                .select("movitv_id")
                .alias("uhr");

        Dataset<Row> otherRatings = ratingsData.alias("or");

        // 找到与用户品味相似的用户（给相同电影高分的用户）
        Dataset<Row> similarUsers = otherRatings
                .join(userHighRatedMovies, otherRatings.col("movitv_id").equalTo(userHighRatedMovies.col("movitv_id")))
                .filter(otherRatings.col("user_id").notEqual(userId)
                        .and(otherRatings.col("rating").geq(7.0)))
                .select(otherRatings.col("user_id").alias("similar_user_id"))
                .distinct();

        // 获取相似用户高分评价的电影
        Dataset<Row> similarUserMovies = ratingsData
                .join(similarUsers, ratingsData.col("user_id").equalTo(similarUsers.col("similar_user_id")))
                .filter(col("rating").geq(7.0))
                .select(ratingsData.col("movitv_id"))
                .distinct();

        // 排除用户已评分的电影
        return similarUserMovies.except(userRatedMovies);
    }

    /**
     * 策略2：获取所有用户评分高的电影
     */
    private Dataset<Row> getAllUsersHighRatedMovies(Dataset<Row> ratingsData, Dataset<Row> userRatedMovies) {
        // 降低阈值，只要评分大于等于6分就考虑
        return ratingsData
                .filter(col("rating").geq(6.0))
                .groupBy("movitv_id")
                .agg(
                    functions.avg("rating").as("avg_rating"),
                    functions.count("rating").as("rating_count")
                )
                .filter(col("rating_count").geq(1))  // 降低最小评分数要求
                .orderBy(
                    col("avg_rating").desc(),
                    col("rating_count").desc()
                )
                .select("movitv_id");
    }

    private Ratings getRatingsByMovieTvId(Long movieTvId) {
        // 获取评分最高的一条记录
        QueryWrapper<Ratings> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("movitv_id", movieTvId)
                   .orderByDesc("rating")
                   .last("LIMIT 1");
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public boolean save(Ratings ratings) {
        boolean result = super.save(ratings);
        if (result) {
            // 保存成功后触发离线推荐检查
            recommendResultService.checkAndTriggerRecommend(ratings.getUserId());
        }
        return result;
    }
}

