package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomData {
    private String roomId; // 房间ID
    private String from;        // 用户ID
    private String to;          // 目标用户ID
}