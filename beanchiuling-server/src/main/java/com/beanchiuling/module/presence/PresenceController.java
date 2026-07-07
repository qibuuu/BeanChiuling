package com.beanchiuling.module.presence;

import com.beanchiuling.common.response.ApiResponse;
import com.beanchiuling.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Void>> heartbeat(@AuthenticationPrincipal User user) {
        presenceService.heartbeat(user.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/bulk")
    public ResponseEntity<ApiResponse<Map<String, String>>> getBulk(
            @RequestParam List<String> userIds) {
        return ResponseEntity.ok(ApiResponse.success(presenceService.getBulkStatus(userIds)));
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            String email = principal.getName();
            // In full implementation: load userId by email from cache
            log.debug("WebSocket connected: {}", email);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            log.debug("WebSocket disconnected: {}", principal.getName());
        }
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(PresenceController.class);
}
