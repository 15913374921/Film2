package com.example.partner.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.partner.entity.RecommendResult;

public interface IRecommendResultService extends IService<RecommendResult> {
    /**
     * 执行离线推荐计算
     */
    void executeOfflineRecommend();

    boolean triggerOfflineRecommend(boolean force);

    void checkAndTriggerRecommend(Integer userId);

    /**
     * 获取用户的推荐结果
     * @param userId 用户ID
     * @param mediaType 媒体类型
     * @return 推荐结果
     */
    RecommendResult getUserRecommendations(String userId, String mediaType);
} 