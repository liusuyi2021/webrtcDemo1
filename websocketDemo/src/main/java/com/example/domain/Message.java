package com.example.domain;

import lombok.Data;

/**
 * @ClassName:Message
 * @Description:
 * @Author:ard
 * @Date:2024年06月15日11:09
 * @Version:1.0
 **/
@Data
public class Message {
    String roomId;
    String userId;
    String message;
    String type;
}
