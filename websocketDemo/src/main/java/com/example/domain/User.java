package com.example.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName:User
 * @Description:
 * @Author:ard
 * @Date:2024年06月23日9:19
 * @Version:1.0
 **/
@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {
    String userId;
    String nickName;
    String roomId;
    String targetUserId;
}
