package com.example.partner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.partner.entity.CommentLike;
import com.example.partner.entity.Comments;
import com.example.partner.entity.User;
import com.example.partner.exception.ServiceException;
import com.example.partner.mapper.CommentLikeMapper;
import com.example.partner.mapper.CommentsMapper;
import com.example.partner.service.ICommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.partner.service.IUserService;
import com.example.partner.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 影视评论表 服务实现类
 * </p>
 *
 * @author LZY
 * @since 2024-12-05
 */
@Slf4j
@Service
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments> implements ICommentsService {
    @Resource
    private CommentsMapper commentsMapper;

    @Resource
    private CommentLikeMapper commentLikeMapper;

    @Resource
    private IUserService userService;

    // Redis键前缀
    private final String COMMENT_CACHE_KEY = "comment:list:%s:%d"; // mediaType:mediaId
    private final String COMMENT_LIKE_KEY = "comment:like:%d";    // commentId

    @Override
    public Comments addComment(Comments comment) {
        log.info("开始添加评论 - 用户ID: {}, 媒体类型: {}, 媒体ID: {}",
                comment.getUserId(), comment.getMediaType(), comment.getMediaId());

        // 1. 保存到MySQL
        int result = commentsMapper.insert(comment);
        log.info("评论保存到MySQL - 评论ID: {}, 结果: {}", comment.getId(), result);

        // 2. 删除Redis缓存
        String cacheKey = String.format(COMMENT_CACHE_KEY,
                comment.getMediaType(), comment.getMediaId());
        RedisUtils.deleteObject(cacheKey);
        log.info("删除Redis缓存 - 键: {}", cacheKey);

        return comment;
    }

    @Override
    public List<Comments> getComments(String mediaType, Long mediaId) {
        String cacheKey = String.format(COMMENT_CACHE_KEY, mediaType, mediaId);
        log.info("获取评论列表 - 媒体类型: {}, 媒体ID: {}", mediaType, mediaId);

        // 1. 尝试从Redis获取缓存
        List<Comments> cachedComments = RedisUtils.getCacheObject(cacheKey);
        if (cachedComments != null) {
            log.info("从Redis缓存获取评论列表 - 数量: {}", cachedComments.size());
            return cachedComments;
        }

        // 2. 从MySQL查询主评论（没有parentId的评论）
        LambdaQueryWrapper<Comments> wrapper = new LambdaQueryWrapper<Comments>()
                .eq(Comments::getMediaType, mediaType)
                .eq(Comments::getMediaId, mediaId)
                .isNull(Comments::getParentId)  // 只查询主评论
                .orderByDesc(Comments::getCreateTime);

        List<Comments> comments = commentsMapper.selectList(wrapper);
        log.info("从MySQL查询主评论列表 - 数量: {}", comments.size());

        // 3. 查询每个主评论的回复
        for (Comments comment : comments) {
            LambdaQueryWrapper<Comments> replyWrapper = new LambdaQueryWrapper<Comments>()
                    .eq(Comments::getParentId, comment.getId())
                    .orderByAsc(Comments::getCreateTime);
            List<Comments> replies = commentsMapper.selectList(replyWrapper);
            // 使用反射或其他方式设置replies字段
            try {
                java.lang.reflect.Field repliesField = comment.getClass().getDeclaredField("replies");
                repliesField.setAccessible(true);
                repliesField.set(comment, replies);
            } catch (Exception e) {
                log.error("设置回复列表失败", e);
            }
        }

        // 4. 存入Redis缓存
        RedisUtils.setCacheObject(cacheKey, comments, 30, TimeUnit.MINUTES);
        log.info("评论列表已缓存到Redis - 键: {}, 过期时间: 30分钟", cacheKey);

        return comments;
    }

    @Override
    @Transactional
    public String likeComment(Long commentId, Integer userId) {
        // 检查是否已经点赞
        QueryWrapper<CommentLike> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("comment_id", commentId)
                   .eq("user_id", userId);

        if (commentLikeMapper.selectCount(queryWrapper) > 0) {
            throw new ServiceException("您已经点赞过该评论");
        }

        // 获取评论信息，包括作者ID
        Comments comment = getById(commentId);
        if (comment == null) {
            throw new ServiceException("评论不存在");
        }

        // 创建点赞记录
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        commentLikeMapper.insert(like);

        // 更新评论的点赞数
        comment.setLikes(comment.getLikes() + 1);
        updateById(comment);

        // 更新评论作者的积分
        double scope = 0.5;  // 每次被点赞加0.5分
        String authorId = comment.getUserId();  // 获取评论作者ID
        User author = userService.getById(authorId);
        if (author != null) {
            double newScope = author.getScope() + scope;
            author.setScope(newScope);
            userService.updateById(author);
            return "点赞成功";
        }

        return "点赞成功";
    }
}
