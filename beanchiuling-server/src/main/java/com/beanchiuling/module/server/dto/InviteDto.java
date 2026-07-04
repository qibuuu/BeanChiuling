package com.beanchiuling.module.server.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class InviteDto {
    private String code;
    private String serverId;
    private String serverName;
    private String serverIconUrl;
    private int memberCount;
    private String inviterUsername;
    private Integer maxUses;
    private int currentUses;
    private Instant expiresAt;
    private Instant createdAt;
}
