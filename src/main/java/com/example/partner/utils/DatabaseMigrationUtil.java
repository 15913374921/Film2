package com.example.partner.utils;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.partner.entity.*;
import com.example.partner.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class DatabaseMigrationUtil {

    @Resource
    private UserMapper userMapper;
    
    @Resource
    private CommentsMapper commentsMapper;
    
    @Resource
    private CommentLikeMapper commentLikeMapper;
    
    @Resource
    private RatingsMapper ratingsMapper;
    
    @Resource
    private RecommendResultMapper recommendResultMapper;

    @Transactional
    public void migrateData() {
        try {
            // 1. 迁移用户数据
            List<User> users = userMapper.selectList(Wrappers.emptyWrapper());
            for (User user : users) {
                try {
                    userMapper.insert(user);
                    log.info("迁移用户数据成功: {}", user.getUsername());
                } catch (Exception e) {
                    log.error("迁移用户数据失败: {}", user.getUsername(), e);
                }
            }

            // 2. 迁移评论数据
            List<Comments> comments = commentsMapper.selectList(Wrappers.emptyWrapper());
            for (Comments comment : comments) {
                try {
                    commentsMapper.insert(comment);
                    log.info("迁移评论数据成功: ID={}", comment.getId());
                } catch (Exception e) {
                    log.error("迁移评论数据失败: ID={}", comment.getId(), e);
                }
            }

            // 3. 迁移评论点赞数据
            List<CommentLike> likes = commentLikeMapper.selectList(Wrappers.emptyWrapper());
            for (CommentLike like : likes) {
                try {
                    commentLikeMapper.insert(like);
                    log.info("迁移点赞数据成功: ID={}", like.getId());
                } catch (Exception e) {
                    log.error("迁移点赞数据失败: ID={}", like.getId(), e);
                }
            }

            // 4. 迁移评分数据
            List<Ratings> ratings = ratingsMapper.selectList(Wrappers.emptyWrapper());
            for (Ratings rating : ratings) {
                try {
                    ratingsMapper.insert(rating);
                    log.info("迁移评分数据成功: ID={}", rating.getId());
                } catch (Exception e) {
                    log.error("迁移评分数据失败: ID={}", rating.getId(), e);
                }
            }

            // 5. 迁移推荐结果数据
            List<RecommendResult> results = recommendResultMapper.selectList(Wrappers.emptyWrapper());
            for (RecommendResult result : results) {
                try {
                    recommendResultMapper.insert(result);
                    log.info("迁移推荐结果数据成功: ID={}", result.getId());
                } catch (Exception e) {
                    log.error("迁移推荐结果数据失败: ID={}", result.getId(), e);
                }
            }

            log.info("数据迁移完成");
        } catch (Exception e) {
            log.error("数据迁移过程中发生错误", e);
            throw e;
        }
    }
} 