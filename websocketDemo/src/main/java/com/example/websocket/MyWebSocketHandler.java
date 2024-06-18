package com.example.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.domain.Message;
import com.example.util.WebSocketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.websocket.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MyWebSocketHandler extends TextWebSocketHandler {


    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        System.out.println("Receive: " + session.getId() +"-->"+ textMessage.getPayload());
        Map jsonMap = JSONObject.parseObject(textMessage.getPayload(),Map.class);
        String type = (String) jsonMap.get("type");
        String content = (String) jsonMap.get("content");
        String userId = (String) session.getAttributes().get("userId");
        String roomId = (String) session.getAttributes().get("roomId");
        log.info("房间[" + roomId + "]收到用户[" + userId + "]消息：" + content);
        Set<String> userSet = WebSocketUtils.ROOM_USER_SET.getOrDefault(roomId, new HashSet<>());
        if (userSet.size() > 0) {
            Map<String, Object> messages = new HashMap<>();
            messages.put("type", type);
            messages.put("roomId", roomId);
            messages.put("userId", userId);
            messages.put("users", userSet);
            messages.put("message", content);
            WebSocketUtils.sendMessageAll(roomId, messages);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Connected: " + session.getId());
        String userId = (String) session.getAttributes().get("userId");
        String roomId = (String) session.getAttributes().get("roomId");
        // 处理接收到的消息
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
        messages.put("roomId", roomId);
        messages.put("userId", userId);
        messages.put("users", userSet);
        messages.put("message", message);

        WebSocketUtils.sendMessageAll(roomId, messages);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("Disconnected: " + session.getId());
        String userId = (String) session.getAttributes().get("userId");
        String roomId = (String) session.getAttributes().get("roomId");
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
        messages.put("roomId", roomId);
        messages.put("userId", userId);
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
}