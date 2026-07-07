package com.beanchiuling.module.message.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "channel_time_idx", def = "{'channelId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "conversation_time_idx", def = "{'conversationId': 1, 'createdAt': -1}")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Message {

    @Id
    private String id;

    @Field("channel_id")
    private String channelId;

    @Field("conversation_id")
    private String conversationId;

    private AuthorInfo author;

    private String content;

    @Builder.Default
    private MessageType type = MessageType.DEFAULT;

    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @Builder.Default
    private List<Reaction> reactions = new ArrayList<>();

    private ReplyInfo replyTo;

    @Field("is_pinned")
    @Builder.Default
    private boolean isPinned = false;

    @Field("is_edited")
    @Builder.Default
    private boolean isEdited = false;

    @Field("edited_at")
    private Instant editedAt;

    @Field("is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @Field("created_at")
    private Instant createdAt;

    private String nonce;

    public enum MessageType { DEFAULT, REPLY, SYSTEM }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuthorInfo {
        @Field("user_id")
        private String userId;
        private String username;
        @Field("display_name")
        private String displayName;
        @Field("avatar_url")
        private String avatarUrl;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Attachment {
        private String id;
        private String url;
        private String filename;
        @Field("content_type")
        private String contentType;
        private long size;
        private Integer width;
        private Integer height;
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Reaction {
        private String emoji;
        @Builder.Default
        private int count = 0;
        @Field("user_ids")
        @Builder.Default
        private List<String> userIds = new ArrayList<>();
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ReplyInfo {
        @Field("message_id")
        private String messageId;
        @Field("author_username")
        private String authorUsername;
        @Field("content_preview")
        private String contentPreview;
    }
}
