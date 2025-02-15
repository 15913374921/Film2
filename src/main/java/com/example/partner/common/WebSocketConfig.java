package com.example.partner.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import com.example.partner.service.ICommentsService;

@Configuration
public class WebSocketConfig {

    private static ICommentsService commentsService;

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 注入 CommentsService
     */
    public WebSocketConfig(ICommentsService commentsService) {
        WebSocketConfig.commentsService = commentsService;
    }

    /**
     * 获取 CommentsService
     */
    public static ICommentsService getCommentsService() {
        return commentsService;
    }
}