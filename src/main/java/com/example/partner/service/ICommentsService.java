package com.example.partner.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.partner.entity.Comments;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * <p>
 * 影视评论表 服务类
 * </p>
 *
 * @author LZY
 * @since 2024-12-05
 */
public interface ICommentsService extends IService<Comments> {

    // 添加评论
    Comments addComment(Comments comment);

    // 获取评论列表
    List<Comments> getComments(String mediaType, Long mediaId);

    // 点赞并增加用户积分
    String likeComment(Long commentId, Integer userId);

}
