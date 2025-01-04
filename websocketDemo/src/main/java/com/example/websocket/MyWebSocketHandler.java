package com.example.websocket;

import com.alibaba.fastjson2.JSONObject;
import com.example.domain.User;
import com.example.util.WebSocketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class MyWebSocketHandler extends TextWebSocketHandler {


    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage textMessage) {
        System.out.println("Receive: " + session.getId() + "-->" + textMessage.getPayload());
        Map jsonMap = JSONObject.parseObject(textMessage.getPayload(), Map.class);
        String type = (String) jsonMap.get("type");
        String roomId = (String) jsonMap.get("roomId");
        String targetUserId = (String) jsonMap.get("targetUserId");
        String userId = (String) session.getAttributes().get("userId");
        String nickName = (String) session.getAttributes().get("nickName");
        String content = (String) jsonMap.get("content");
        switch (type) {
            case "invite":
                User user = new User(userId, nickName, roomId, targetUserId);
                inviteRoom(user);
                break;
            case "join":
                user = new User(userId, nickName, roomId, targetUserId);
                joinRoom(user);
                break;
            case "leave":
                user = new User(userId, nickName, roomId, targetUserId);
                leaveRoom(user);
                break;
            default:
                break;
        }
        if (roomId != null) {
            Set<User> userSet = WebSocketUtils.ROOM_USER_SET.getOrDefault(roomId, new HashSet<>());
            Map<String, Object> messages = new HashMap<>();
            messages.put("type", type);
            messages.put("roomId", roomId);
            messages.put("userId", userId);
            messages.put("users", userSet);
            messages.put("targetUserId", targetUserId);
            messages.put("content", content);
            if (targetUserId != null) {
                WebSocketSession targetSession = WebSocketUtils.ONLINE_USER_SESSIONS.get(targetUserId);
                WebSocketUtils.sendMessage(targetSession, messages);
            } else {
                WebSocketUtils.sendMessageAll(roomId, messages);
            }
        }
        //  log.info("房间[" + roomId + "]收到用户[" + userId + "]消息：" + content);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        // 处理接收到的消息
        WebSocketUtils.ONLINE_USER_SESSIONS.put(userId, session);

        WebSocketUtils.sendMessageAll(userId);
        System.out.println("Connected: " + session.getId() + "[" + userId + "]");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        //当前的Session 移除
        WebSocketUtils.ONLINE_USER_SESSIONS.remove(userId);
        try {
            session.close();
        } catch (IOException e) {
            log.error("onClose error", e);
        }
        System.out.println("Disconnected: " + session.getId() + "[" + userId + "]");
    }

    //处理邀请进入房间
    private void inviteRoom(User user) {
        Set<User> userSet = WebSocketUtils.ROOM_USER_SET.getOrDefault(user.getRoomId(), new HashSet<>());
        WebSocketUtils.ROOM_USER_SET.put(user.getRoomId(), userSet);
        String message =
                "用户[" + user.getUserId() + "]邀请[" + user.getTargetUserId() + "]进入房间[" + user.getRoomId() + "]用户总数：" + userSet.size();
        Map<String, Object> messages = new HashMap<>();
        messages.put("type", "invite");
        messages.put("roomId", user.getRoomId());
        messages.put("userId", user.getUserId());
        messages.put("targetUserId", user.getTargetUserId());
        messages.put("users", userSet);
        messages.put("content", message);
       // WebSocketUtils.sendMessageAll(messages);
        log.info(message);
    }

    private void joinRoom(User user) {
        Set<User> userSet = WebSocketUtils.ROOM_USER_SET.getOrDefault(user.getRoomId(), new HashSet<>());
        userSet.add(user);
        WebSocketUtils.ROOM_USER_SET.put(user.getRoomId(), userSet);
        String message = "用户[" + user.getUserId() + "]进入了房间[" + user.getRoomId() + "]用户总数：" + userSet.size();
        Map<String, Object> messages = new HashMap<>();
        messages.put("type", "join");
        messages.put("roomId", user.getRoomId());
        messages.put("userId", user.getUserId());
        messages.put("users", userSet);
        messages.put("content", message);
        WebSocketUtils.sendMessageAll(user.getRoomId(), messages);
        log.info(message);
    }

    private void leaveRoom(User user) {
        Set<User> userSet = WebSocketUtils.ROOM_USER_SET.getOrDefault(user.getRoomId(), new HashSet<>());
        userSet.remove(user);
        //并且通知其他人当前用户已经离开聊天室了
        String message = "用户[" + user.getUserId() + "]离开房间[" + user.getRoomId() + "]";
        Map<String, Object> messages = new HashMap<>();
        messages.put("type", "leave");
        messages.put("roomId", user.getRoomId());
        messages.put("userId", user.getUserId());
        messages.put("users", userSet);
        messages.put("content", message);
        WebSocketUtils.sendMessageAll(messages);
        log.info("消息：" + message);
    }
}