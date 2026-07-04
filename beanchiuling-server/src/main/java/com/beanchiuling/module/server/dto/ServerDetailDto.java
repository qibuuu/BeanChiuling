package com.beanchiuling.module.server.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ServerDetailDto {

    private String id;
    private String name;
    private String description;
    private String iconUrl;
    private String bannerUrl;
    private String inviteCode;
    private boolean isPublic;
    private int maxMembers;
    private int memberCount;
    private Instant createdAt;
    private OwnerInfo owner;
    private List<MemberInfo> members;

    @Getter
    @Builder
    public static class OwnerInfo {
        private String id;
        private String username;
        private String displayName;
        private String avatarUrl;
    }

    @Getter
    @Builder
    public static class MemberInfo {
        private String memberId;
        private String userId;
        private String username;
        private String displayName;
        private String avatarUrl;
        private String nickname;
        private Instant joinedAt;
    }
}
