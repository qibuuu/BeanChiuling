package com.beanchiuling.module.channel;

import com.beanchiuling.common.response.ApiResponse;
import com.beanchiuling.module.channel.dto.ChannelDto;
import com.beanchiuling.module.channel.dto.CreateChannelRequest;
import com.beanchiuling.module.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/servers/{serverId}/channels")
    public ResponseEntity<ApiResponse<ChannelDto>> createChannel(
            @AuthenticationPrincipal User user,
            @PathVariable String serverId,
            @Valid @RequestBody CreateChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(channelService.createChannel(serverId, user.getId(), request)));
    }

    @GetMapping("/servers/{serverId}/channels")
    public ResponseEntity<ApiResponse<List<ChannelDto>>> getChannels(
            @AuthenticationPrincipal User user,
            @PathVariable String serverId) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getServerChannels(serverId, user.getId())));
    }

    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(
            @AuthenticationPrincipal User user,
            @PathVariable String channelId) {
        channelService.deleteChannel(channelId, user.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
