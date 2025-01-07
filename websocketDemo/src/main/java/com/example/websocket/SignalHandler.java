package com.example.websocket;

import com.alibaba.fastjson2.JSON;
import com.example.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SignalHandler extends TextWebSocketHandler {

    // 房间信息
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    // 存储 websocket session
    public static final ConcurrentHashMap<String, WebSocketSession> ONLINE_USER_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        ONLINE_USER_SESSIONS.put(userId, session);
        System.out.println("User：[" + userId + "] Connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 解析消息为 WebSocketMessage 实体
        WebSocketMessage msg = JSON.parseObject(message.getPayload(), WebSocketMessage.class);
        String from = (String) session.getAttributes().get("userId");
        switch (msg.getType()) {
            case "call":
                CallData callData = JSON.parseObject(JSON.toJSONString(msg.getData()), CallData.class);
                callData.setFrom(from);
                handleCall(session, callData);
                break;
            case "accept":
                AcceptData acceptData = JSON.parseObject(JSON.toJSONString(msg.getData()),
                        AcceptData.class);
                acceptData.setFrom(from);
                handleAccept(session, acceptData);
                break;
            case "hungUp":
                HungUpData hungUpData = JSON.parseObject(JSON.toJSONString(msg.getData()),
                        HungUpData.class);
                hungUpData.setFrom(from);
                handleHungUp(session, hungUpData);
                break;
            case "createRoom":
                CreateRoomData createRoomData = JSON.parseObject(JSON.toJSONString(msg.getData()),
                        CreateRoomData.class);
                createRoomData.setFrom(from);
                handleCreateRoom(session, createRoomData);
                break;
            case "joinRoom":
                JoinRoomData joinRoomData = JSON.parseObject(JSON.toJSONString(msg.getData()), JoinRoomData.class);
                joinRoomData.setFrom(from);
                handleJoinRoom(session, joinRoomData);
                break;
            case "leaveRoom":
                JoinRoomData leaveRoomData = JSON.parseObject(JSON.toJSONString(msg.getData()), JoinRoomData.class);
                leaveRoomData.setFrom(from);
                handleLeaveRoom(session, leaveRoomData);
                break;
            case "offer":
            case "answer":
            case "iceCandidate":
                SignalData signalData = JSON.parseObject(JSON.toJSONString(msg.getData()), SignalData.class);
                signalData.setFrom(from);
                handleSignal(session, signalData, msg.getType());
                break;
            case "heart":
                HeartData heartData = JSON.parseObject(JSON.toJSONString(msg.getData()), HeartData.class);
                heartData.setFrom(from);
                System.out.println("receive heart: " + heartData.getFrom());
                break;
            default:
                System.out.println("Unknown message type: " + msg.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        rooms.values().forEach(sessions -> sessions.remove(session));
        String userId = (String) session.getAttributes().get("userId");
        ONLINE_USER_SESSIONS.remove(userId);
        System.out.println("User：[" + userId + "] Connection closed: " + session.getId());
    }

    // 处理呼叫
    private void handleCall(WebSocketSession session, CallData data) {
        String userId = data.getTo();
        sendMessageToUser(userId, new WebSocketMessage("call", data));
        log.info("用户{}呼叫用户{}", session.getAttributes().get("userId"), userId);
    }

    // 处理同意
    private void handleAccept(WebSocketSession session, AcceptData data) {
        String roomId = data.getRoomId();
        // 通知房间其他用户
        broadcastMessage(roomId, new WebSocketMessage("accept", data), session);
        log.info("用户{}回复同意", session.getAttributes().get("userId"));
    }

    // 处理挂断
    private void handleHungUp(WebSocketSession session, HungUpData data) {
        String roomId = data.getRoomId();
        // 通知房间其他用户
        broadcastMessage(roomId, new WebSocketMessage("hungUp", data), session);
        log.info("用户{}挂断了", session.getAttributes().get("userId"));
    }

    // 处理创建房间
    private void handleCreateRoom(WebSocketSession session, CreateRoomData createRoomData) {
        String roomId = UUID.randomUUID().toString();
        rooms.put(roomId, ConcurrentHashMap.newKeySet());
        rooms.get(roomId).add(session);
        // 回复房间创建成功消息
        createRoomData.setRoomId(roomId);
        sendMessage(session, new WebSocketMessage("roomCreated", createRoomData));
        log.info("用户{}创建了房间{}", session.getAttributes().get("userId"), roomId);
    }

    // 处理加入房间
    private void handleJoinRoom(WebSocketSession session, JoinRoomData data) {
        String roomId = data.getRoomId();
        if (!rooms.containsKey(roomId)) {
            sendMessage(session, new WebSocketMessage("error", "Room not found"));
            return;
        }

        rooms.get(roomId).add(session);
        String from = (String) session.getAttributes().get("userId");
        data.setFrom(from);
        // 通知房间其他用户
        broadcastMessage(roomId, new WebSocketMessage("userJoined", data), session);
        log.info("用户{}加入了房间{}", session.getAttributes().get("userId"), roomId);
    }

    // 处理离开房间
    private void handleLeaveRoom(WebSocketSession session, JoinRoomData data) {
        String roomId = data.getRoomId();
        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).remove(session);

            // 通知房间其他用户
            broadcastMessage(roomId, new WebSocketMessage("userLeaved", data), session);
            log.info("用户{}离开了房间{}", session.getAttributes().get("userId"), roomId);
        }
    }

    // 处理信令
    private void handleSignal(WebSocketSession session, SignalData data, String type) {
        String roomId = data.getRoomId();
        if (rooms.containsKey(roomId)) {
            // 转发信令消息
            broadcastMessage(roomId, new WebSocketMessage(type, data), session);
        }
    }

    /**
     * 向指定会话发送消息
     *
     * @param session 会话
     * @param message 消息
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                String jsonMessage = JSON.toJSONString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            }
        } catch (Exception e) {
            System.err.println("Failed to send message to session " + session.getId() + ": " + e.getMessage());
        }
    }

    /**
     * 向房间内广播消息
     *
     * @param roomId  房间ID
     * @param message 消息
     * @param exclude 排除的会话（可为 null）
     */
    private void broadcastMessage(String roomId, WebSocketMessage message, WebSocketSession exclude) {
        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).forEach(session -> {
                if (!session.equals(exclude)) {
                    sendMessage(session, message);
                }
            });
        }
    }

    /**
     * 向房间内广播消息（不排除任何会话）
     *
     * @param roomId  房间ID
     * @param message 消息
     */
    private void broadcastMessage(String roomId, WebSocketMessage message) {
        broadcastMessage(roomId, message, null);
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId  用户ID
     * @param message 消息
     */
    private void sendMessageToUser(String userId, WebSocketMessage message) {
        // 获取用户对应的 WebSocketSession
        WebSocketSession session = ONLINE_USER_SESSIONS.get(userId);
        if (session != null) {
            // 如果用户的 WebSocketSession 存在，发送消息
            sendMessage(session, message);
        } else {
            // 如果没有找到该用户的 session，可能需要处理用户离线的情况
            System.out.println("User with ID " + userId + " is not online.");
        }
    }

}
