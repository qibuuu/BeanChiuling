package com.beanchiuling.module.friend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter @Builder
public class FriendDto {
    private String friendshipId;
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String status;     // PENDING / ACCEPTED / BLOCKED
    private boolean incoming;  // true nếu mình là người nhận yêu cầu
    private Instant since;
}
