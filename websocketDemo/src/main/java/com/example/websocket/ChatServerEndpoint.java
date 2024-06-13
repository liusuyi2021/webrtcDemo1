package com.example.websocket;

import com.example.util.WebSocketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName ChatServerEndpoint
 * @Description: websocket操作类
 * @Author 刘苏义
 * @Date 2023/1/27 7:42
 * @Version 1.0
 */

@Component
@Slf4j(topic = "websocket")
@ServerEndpoint("/ws/{roomId}/{userId}")
public class ChatServerEndpoint {


    @OnOpen
    public void openSession(@PathParam("roomId") String roomId, @PathParam("userId") String userId, Session session) {
        WebSocketUtils.ONLINE_USER_SESSIONS.put(userId, session);
        // 获取房间用户列表
        Set<String> users = WebSocketUtils.ROOM_USER_SET.getOrDefault(roomId, new HashSet<>());
        users.add(userId);
        WebSocketUtils.ROOM_USER_SET.put(roomId, users);
        String message = "用户[" + userId + "]进入了房间[" + roomId + "]";
        log.info(message);
        Set<String> userSet = WebSocketUtils.ROOM_USER_SET.get(roomId);
        log.info("房间[" + roomId + "]用户总数：" + userSet.size());
        Map<String, Object> messages = new HashMap<>();
        messages.put("type", "join");
        messages.put("room", roomId);
        messages.put("user", userId);
        messages.put("users", userSet);
        messages.put("message", message);

        WebSocketUtils.sendMessageAll(roomId, messages);
    }

    @OnMessage
    public void onMessage(@PathParam("roomId") String roomId, @PathParam("userId") String userId, String message) {
        log.info("房间[" + roomId + "]收到用户[" + userId + "]消息：" + message);
        Set<String> userSet = WebSocketUtils.ROOM_USER_SET.getOrDefault(roomId, new HashSet<>());
        if(userSet.size()>0) {
            Map<String, Object> messages = new HashMap<>();
            messages.put("type", "message");
            messages.put("room", roomId);
            messages.put("user", userId);
            messages.put("users", userSet);
            messages.put("message", message);
            WebSocketUtils.sendMessageAll(roomId, messages);
        }
    }

    @OnClose
    public void onClose(@PathParam("roomId") String roomId, @PathParam("userId") String userId, Session session) {
        //当前的Session 移除
        WebSocketUtils.ONLINE_USER_SESSIONS.remove(userId);
        Set<String> userSet = WebSocketUtils.ROOM_USER_SET.getOrDefault(roomId, new HashSet<>());
        if (userSet.size() > 0) {
            userSet.remove(userId);
            WebSocketUtils.ROOM_USER_SET.put(roomId, userSet);
        }

        //并且通知其他人当前用户已经离开聊天室了
        String message = "用户[" + userId + "]离开房间[" + roomId + "]";
        Map<String, Object> messages = new HashMap<>();
        messages.put("type", "leave");
        messages.put("room", roomId);
        messages.put("user", userId);
        messages.put("users", userSet);
        messages.put("message", message);
        WebSocketUtils.sendMessageAll(roomId,messages);
        log.info("消息：" + message);
        try {
            session.close();
        } catch (IOException e) {
            log.error("onClose error", e);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        try {
            session.close();
        } catch (IOException e) {
            log.error("onError excepiton", e);
        }
        log.info("Throwable msg " + throwable.getMessage());
    }

}