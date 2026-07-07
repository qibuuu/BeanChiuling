package com.beanchiuling.module.message;

import com.beanchiuling.common.response.ApiResponse;
import com.beanchiuling.module.message.dto.MessageDto;
import com.beanchiuling.module.message.dto.SendMessageRequest;
import com.beanchiuling.module.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // ── REST endpoints (HTTP) ──────────────────────────────────────────────

    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @AuthenticationPrincipal User user,
            @PathVariable String channelId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(messageService.sendMessage(channelId, user, request)));
    }

    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
            @AuthenticationPrincipal User user,
            @PathVariable String channelId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                messageService.getChannelHistory(channelId, user.getId(), before, limit)));
    }

    @PatchMapping("/channels/{channelId}/messages/{messageId}")
    public ResponseEntity<ApiResponse<MessageDto>> editMessage(
            @AuthenticationPrincipal User user,
            @PathVariable String channelId,
            @PathVariable String messageId,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                messageService.editMessage(messageId, user.getId(), body.get("content"))));
    }

    @DeleteMapping("/channels/{channelId}/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @AuthenticationPrincipal User user,
            @PathVariable String channelId,
            @PathVariable String messageId) {
        messageService.deleteMessage(messageId, user.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    // ── WebSocket endpoints (STOMP) ────────────────────────────────────────
    // Client subscribe: /topic/channel.{channelId}
    // Client send:      /app/channel.{channelId}.typing

    @MessageMapping("/channel.{channelId}.typing")
    public void handleTyping(
            @DestinationVariable String channelId,
            Principal principal) {
        // Principal.getName() = email — load minimal user info from security context
        // Full implementation injects SecurityContextHolder
    }
}
