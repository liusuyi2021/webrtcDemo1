package com.example.util;

import com.alibaba.fastjson2.JSONObject;
import com.example.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName WebSocketUtils
 * @Description: websocket工具类
 * @Author 刘苏义
 * @Date 2023/1/27 7:46
 * @Version 1.0
 */
@Slf4j
public final class WebSocketUtils {

    // 存储 websocket session
    public static final ConcurrentHashMap<String, WebSocketSession> ONLINE_USER_SESSIONS = new ConcurrentHashMap<>();
    //存储房间
    public static final ConcurrentHashMap<String, Set<User>> ROOM_USER_SET= new ConcurrentHashMap<>();
    /**
     * @param session 用户 session
     * @param message 发送内容
     */
    public static void sendMessage(WebSocketSession session, String message) {
        if (session == null) {
            return;
        }
        synchronized(session) {
            try {
                log.debug("发送消息："+message);
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("sendMessage IOException ",e);
            }
        }
    }
    /**
     * @param session 用户 session
     * @param message 发送内容
     */
    public static void sendMessage(WebSocketSession session, Map message) {
        if (session == null) {
            return;
        }

        synchronized(session) {
            try {
                session.sendMessage(new TextMessage(JSONObject.toJSONString(message)));
            } catch (IOException e) {
                log.error("sendMessage IOException ",e);
            }
        }
    }
    public static void sendMessage(WebSocketSession session, List message) {
        if (session == null) {
            return;
        }

        synchronized(session) {
            try {
                session.sendMessage( new TextMessage(String.join(", ", message)));
            } catch (IOException e) {
                log.error("sendMessage IOException ",e);
            }
        }
    }
    /**
     * 推送消息到其他客户端
     * @param message
     */
    public static void sendMessageAll(String message) {
        ONLINE_USER_SESSIONS.forEach((sessionId, session) -> sendMessage(session, message));
    }

    /**
     * 推送消息到当前房间的其他客户端
     * @param message
     */
    public static void sendMessageAll(String roomId,String message) {
        Set<User> userSet = ROOM_USER_SET.getOrDefault(roomId, new HashSet<>());
        userSet.stream().forEach(user -> {
            WebSocketSession session = ONLINE_USER_SESSIONS.getOrDefault(user.getUserId(), null);
            if(session!=null)
            {
                sendMessage(session, message);
            }
        });
    }
    /**
     * 推送消息到其他客户端
     * @param message
     */
    public static void sendMessageAll(Map message) {
        JSONObject jsonObject=new JSONObject(message);
        ONLINE_USER_SESSIONS.forEach((sessionId, session) -> sendMessage(session, jsonObject.toString()));
    }
    /**
     * 推送消息到当前房间的其他客户端
     * @param message
     */
    public static void sendMessageAll(String roomId,Map message) {
        Set<User> userSet = ROOM_USER_SET.getOrDefault(roomId, new HashSet<>());
        userSet.stream().forEach(user -> {
            WebSocketSession session = ONLINE_USER_SESSIONS.getOrDefault(user.getUserId(), null);
            if(session!=null)
            {
                sendMessage(session, message);
            }
        });
    }
}