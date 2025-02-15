package com.example.partner.common;

import com.example.partner.service.ICommentsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.partner.entity.Comments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务器端点，用于处理评论的实时推送
 */
@Component
@Slf4j
@ServerEndpoint("/websocket/comment/{mediaType}/{mediaId}/{userId}")
public class WebSocketServer {

    /**
     * 用于存储所有WebSocket连接会话
     * 外层Map的key是 mediaType:mediaId
     * 内层Map的key是 userId，value是用户的WebSocket session
     */
    private static final Map<String, Map<String, Session>> sessionMap = new ConcurrentHashMap<>();

    private ICommentsService commentsService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketServer() {
        this.commentsService = WebSocketConfig.getCommentsService();
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("mediaType") String mediaType,
                      @PathParam("mediaId") String mediaId,
                      @PathParam("userId") String userId) {
        String mediaKey = mediaType + ":" + mediaId;
        log.info("WebSocket连接建立 - 媒体: {}, 用户: {}", mediaKey, userId);

        // 获取或创建该媒体的用户session Map
        Map<String, Session> userSessions = sessionMap.computeIfAbsent(mediaKey, k -> new ConcurrentHashMap<>());
        
        // 添加用户的session
        userSessions.put(userId, session);
        
        log.info("当前媒体在线用户数: {}", userSessions.size());
        log.info("所有在线媒体: {}", sessionMap.keySet());
    }

    @OnClose
    public void onClose(@PathParam("mediaType") String mediaType,
                       @PathParam("mediaId") String mediaId,
                       @PathParam("userId") String userId) {
        String mediaKey = mediaType + ":" + mediaId;
        log.info("WebSocket连接关闭 - 媒体: {}, 用户: {}", mediaKey, userId);

        // 获取该媒体的用户session Map
        Map<String, Session> userSessions = sessionMap.get(mediaKey);
        if (userSessions != null) {
            // 移除用户的session
            userSessions.remove(userId);
            
            // 如果该媒体没有用户了，移除整个媒体Map
            if (userSessions.isEmpty()) {
                sessionMap.remove(mediaKey);
            }
            
            log.info("当前媒体在线用户数: {}", userSessions.size());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到WebSocket消息 - 原始消息内容: {}", message);
        try {
            // 使用 Jackson 直接转换为对象
            Comments comments = objectMapper.readValue(message, Comments.class);
            log.info("转换后的对象: {}", comments);
            
            // 保存评论或回复
            Comments savedComment = commentsService.addComment(comments);
            log.info("评论/回复已保存到数据库 - ID: {}", savedComment.getId());
            
            // 广播消息给同一媒体的所有用户
            String mediaKey = comments.getMediaType() + ":" + comments.getMediaId();
            // 根据是否有parentId来判断是评论还是回复
            String messageType = comments.getParentId() != null ? "reply" : "comment";
            broadcastMessage(mediaKey, savedComment, messageType);
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}", e.getMessage(), e);
            try {
                // 发送错误消息给发送者
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("type", "error");
                errorResponse.put("message", "评论/回复发送失败: " + e.getMessage());
                session.getBasicRemote().sendText(objectMapper.writeValueAsString(errorResponse));
            } catch (Exception ex) {
                log.error("发送错误消息失败", ex);
            }
        }
    }

    /**
     * 广播消息给指定媒体的所有用户
     */
    public void broadcastMessage(String mediaKey, Comments message, String messageType) {
        try {
            // 构建响应对象
            Map<String, Object> response = new HashMap<>();
            response.put("type", messageType);  // "comment" 或 "reply"
            response.put("data", message);
            
            // 转换为JSON
            String messageJson = objectMapper.writeValueAsString(response);
            log.info("准备广播的消息内容: {}", messageJson);
            
            // 获取该媒体的所有用户session
            Map<String, Session> userSessions = sessionMap.get(mediaKey);
            if (userSessions != null && !userSessions.isEmpty()) {
                log.info("找到目标媒体的用户数: {}", userSessions.size());
                
                // 遍历发送给每个用户
                for (Map.Entry<String, Session> entry : userSessions.entrySet()) {
                    Session userSession = entry.getValue();
                    String userId = entry.getKey();
                    
                    if (userSession != null && userSession.isOpen()) {
                        try {
                            userSession.getBasicRemote().sendText(messageJson);
                            log.info("成功发送{}消息到用户: {}", messageType, userId);
                        } catch (Exception e) {
                            log.error("发送消息到用户失败 - userId: {}, 错误: {}", userId, e.getMessage());
                        }
                    } else {
                        log.warn("用户会话无效或已关闭 - userId: {}", userId);
                        userSessions.remove(userId);
                    }
                }
            } else {
                log.warn("未找到目标媒体的任何用户 - mediaKey: {}", mediaKey);
            }
        } catch (Exception e) {
            log.error("广播消息时发生错误: {}", e.getMessage(), e);
        }
    }
}
