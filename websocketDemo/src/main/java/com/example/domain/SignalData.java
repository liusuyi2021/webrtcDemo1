package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalData {
    private String roomId; // 房间ID
    private String from;   // 用户ID
    private String to;     // 目标用户ID
    private String signal; // 信令数据
}