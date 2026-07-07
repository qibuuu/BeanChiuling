package com.beanchiuling.module.friend;

import com.beanchiuling.common.response.ApiResponse;
import com.beanchiuling.module.friend.dto.FriendDto;
import com.beanchiuling.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/users/@me/friends")
    public ResponseEntity<ApiResponse<FriendDto>> sendRequest(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                friendService.sendFriendRequest(user.getId(), body.get("username"))));
    }

    @GetMapping("/users/@me/friends")
    public ResponseEntity<ApiResponse<List<FriendDto>>> getFriends(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getFriends(user.getId())));
    }

    @GetMapping("/users/@me/friends/pending")
    public ResponseEntity<ApiResponse<List<FriendDto>>> getPending(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(friendService.getPendingRequests(user.getId())));
    }

    @PutMapping("/users/@me/friends/{friendshipId}/accept")
    public ResponseEntity<ApiResponse<FriendDto>> accept(
            @AuthenticationPrincipal User user,
            @PathVariable String friendshipId) {
        return ResponseEntity.ok(ApiResponse.success(
                friendService.acceptFriendRequest(friendshipId, user.getId())));
    }

    @DeleteMapping("/users/@me/friends/{friendshipId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @AuthenticationPrincipal User user,
            @PathVariable String friendshipId) {
        friendService.removeFriend(friendshipId, user.getId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
