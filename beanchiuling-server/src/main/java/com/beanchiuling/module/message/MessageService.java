package com.beanchiuling.module.message;

import com.beanchiuling.common.exception.AppException;
import com.beanchiuling.common.exception.ErrorCode;
import com.beanchiuling.common.utils.PermissionUtils;
import com.beanchiuling.module.channel.ChannelRepository;
import com.beanchiuling.module.channel.entity.Channel;
import com.beanchiuling.module.message.document.Message;
import com.beanchiuling.module.message.dto.MessageDto;
import com.beanchiuling.module.message.dto.SendMessageRequest;
import com.beanchiuling.module.message.dto.WsEvent;
import com.beanchiuling.module.server.ServerMemberRepository;
import com.beanchiuling.module.server.entity.Role;
import com.beanchiuling.module.server.entity.Server;
import com.beanchiuling.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChannelRepository channelRepository;
    private final ServerMemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int PAGE_SIZE = 50;

    public MessageDto sendMessage(String channelId, User sender, SendMessageRequest req) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        requireSendPermission(sender.getId(), channel.getServer());

        Message.ReplyInfo replyInfo = null;
        if (req.getReplyToMessageId() != null) {
            replyInfo = messageRepository.findByIdAndIsDeletedFalse(req.getReplyToMessageId())
                    .map(m -> Message.ReplyInfo.builder()
                            .messageId(m.getId())
                            .authorUsername(m.getAuthor().getUsername())
                            .contentPreview(m.getContent().length() > 100
                                    ? m.getContent().substring(0, 100) + "..."
                                    : m.getContent())
                            .build())
                    .orElse(null);
        }

        Message message = Message.builder()
                .channelId(channelId)
                .author(Message.AuthorInfo.builder()
                        .userId(sender.getId())
                        .username(sender.getUsernameValue())
                        .displayName(sender.getDisplayName())
                        .avatarUrl(sender.getAvatarUrl())
                        .build())
                .content(req.getContent())
                .replyTo(replyInfo)
                .nonce(req.getNonce())
                .createdAt(Instant.now())
                .build();

        Message saved = messageRepository.save(message);
        MessageDto dto = toDto(saved);

        // Broadcast tới tất cả clients đang subscribe kênh này qua WebSocket
        messagingTemplate.convertAndSend(
                "/topic/channel." + channelId,
                WsEvent.builder().type(WsEvent.MESSAGE_CREATE).data(dto).build()
        );

        log.debug("Message sent in channel {} by {}", channelId, sender.getUsernameValue());
        return dto;
    }

    public List<MessageDto> getChannelHistory(String channelId, String userId,
                                               String beforeId, int limit) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        requireMembership(userId, channel.getServer().getId());

        int safeLimit = Math.min(limit, 100);
        PageRequest pageable = PageRequest.of(0, safeLimit);

        List<Message> messages = beforeId != null
                ? messageRepository.findByChannelIdAndIdLessThanAndIsDeletedFalseOrderByCreatedAtDesc(
                channelId, beforeId, pageable)
                : messageRepository.findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                channelId, pageable);

        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    public MessageDto editMessage(String messageId, String userId, String newContent) {
        Message message = messageRepository.findByIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getAuthor().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.MESSAGE_FORBIDDEN);
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(Instant.now());

        Message saved = messageRepository.save(message);
        MessageDto dto = toDto(saved);

        messagingTemplate.convertAndSend(
                "/topic/channel." + message.getChannelId(),
                WsEvent.builder().type(WsEvent.MESSAGE_UPDATE).data(dto).build()
        );

        return dto;
    }

    public void deleteMessage(String messageId, String userId) {
        Message message = messageRepository.findByIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        boolean isAuthor = message.getAuthor().getUserId().equals(userId);
        if (!isAuthor) {
            Channel channel = channelRepository.findById(message.getChannelId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
            requirePermission(userId, channel.getServer(), PermissionUtils.MANAGE_MESSAGES);
        }

        message.setDeleted(true);
        messageRepository.save(message);

        messagingTemplate.convertAndSend(
                "/topic/channel." + message.getChannelId(),
                WsEvent.builder().type(WsEvent.MESSAGE_DELETE)
                        .data(java.util.Map.of("messageId", messageId)).build()
        );
    }

    public void broadcastTyping(String channelId, User user) {
        messagingTemplate.convertAndSend(
                "/topic/channel." + channelId,
                WsEvent.builder().type(WsEvent.TYPING_START)
                        .data(java.util.Map.of(
                                "userId", user.getId(),
                                "username", user.getUsernameValue()
                        )).build()
        );
    }

    private void requireMembership(String userId, String serverId) {
        if (!memberRepository.existsByUserIdAndServerId(userId, serverId)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN);
        }
    }

    private void requireSendPermission(String userId, Server server) {
        requirePermission(userId, server, PermissionUtils.SEND_MESSAGES);
    }

    private void requirePermission(String userId, Server server, long flag) {
        if (server.getOwner().getId().equals(userId)) return;
        long perms = memberRepository.findByUserIdAndServerId(userId, server.getId())
                .map(m -> m.getRoles().stream()
                        .mapToLong(Role::getPermissions).reduce(0L, (a, b) -> a | b))
                .orElse(0L);
        if (!PermissionUtils.has(perms, flag)) {
            throw new AppException(ErrorCode.MESSAGE_FORBIDDEN);
        }
    }

    private MessageDto toDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .channelId(m.getChannelId())
                .conversationId(m.getConversationId())
                .author(MessageDto.AuthorDto.builder()
                        .userId(m.getAuthor().getUserId())
                        .username(m.getAuthor().getUsername())
                        .displayName(m.getAuthor().getDisplayName())
                        .avatarUrl(m.getAuthor().getAvatarUrl())
                        .build())
                .content(m.getContent())
                .type(m.getType().name())
                .attachments(m.getAttachments())
                .reactions(m.getReactions())
                .replyTo(m.getReplyTo())
                .isPinned(m.isPinned())
                .isEdited(m.isEdited())
                .editedAt(m.getEditedAt())
                .createdAt(m.getCreatedAt())
                .nonce(m.getNonce())
                .build();
    }
}
