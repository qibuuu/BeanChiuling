package com.beanchiuling.module.server;

import com.beanchiuling.common.response.ApiResponse;
import com.beanchiuling.module.server.dto.*;
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
public class ServerController {

    private final ServerService serverService;

    @PostMapping("/servers")
    public ResponseEntity<ApiResponse<ServerDetailDto>> createServer(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateServerRequest request) {

        ServerDetailDto dto = serverService.createServer(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping("/servers/{serverId}")
    public ResponseEntity<ApiResponse<ServerDetailDto>> getServer(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String serverId) {

        ServerDetailDto dto = serverService.getServer(serverId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/users/@me/servers")
    public ResponseEntity<ApiResponse<List<ServerDetailDto>>> getMyServers(
            @AuthenticationPrincipal User currentUser) {

        List<ServerDetailDto> servers = serverService.getUserServers(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(servers));
    }

    @DeleteMapping("/servers/{serverId}")
    public ResponseEntity<ApiResponse<Void>> deleteServer(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String serverId) {

        serverService.deleteServer(serverId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/servers/{serverId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveServer(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String serverId) {

        serverService.leaveServer(serverId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/servers/{serverId}/members/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String serverId,
            @PathVariable String targetUserId) {

        serverService.kickMember(serverId, targetUserId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/servers/{serverId}/invites")
    public ResponseEntity<ApiResponse<InviteDto>> createInvite(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String serverId,
            @Valid @RequestBody(required = false) CreateInviteRequest request) {

        if (request == null) request = new CreateInviteRequest();
        InviteDto dto = serverService.createInvite(serverId, currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @GetMapping("/invites/{code}")
    public ResponseEntity<ApiResponse<InviteDto>> getInvite(
            @PathVariable String code) {

        InviteDto dto = serverService.getInvite(code);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/invites/{code}")
    public ResponseEntity<ApiResponse<ServerDetailDto>> joinByInvite(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String code) {

        ServerDetailDto dto = serverService.joinByInviteCode(code, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
