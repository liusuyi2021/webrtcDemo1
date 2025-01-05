package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // 消息类型，如 "createRoom", "offer", "answer"
    private Object data; // 数据载体，可以是任意实体类
}
