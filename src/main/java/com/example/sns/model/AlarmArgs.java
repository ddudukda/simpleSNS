package com.example.sns.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlarmArgs {

    private Integer fromUserId;  // alarm을 발생시킨 user
    private Integer targetId;    // alarm을 받는 user

}
