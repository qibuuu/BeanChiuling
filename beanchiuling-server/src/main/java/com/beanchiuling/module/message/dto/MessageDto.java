package com.beanchiuling.module.message.dto;

import com.beanchiuling.module.message.document.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter @Builder
public class MessageDto {
    private String id;
    private String channelId;
    private String conversationId;
    private AuthorDto author;
    private String content;
    private String type;
    private List<Message.Attachment> attachments;
    private List<Message.Reaction> reactions;
    private Message.ReplyInfo replyTo;
    private boolean isPinned;
    private boolean isEdited;
    private Instant editedAt;
    private Instant createdAt;
    private String nonce;

    @Getter @Builder
    public static class AuthorDto {
        private String userId;
        private String username;
        private String displayName;
        private String avatarUrl;
    }
}
