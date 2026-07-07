package com.beanchiuling.module.friend;

import com.beanchiuling.common.exception.AppException;
import com.beanchiuling.common.exception.ErrorCode;
import com.beanchiuling.module.friend.dto.FriendDto;
import com.beanchiuling.module.friend.entity.DirectConversation;
import com.beanchiuling.module.friend.entity.Friendship;
import com.beanchiuling.module.friend.entity.Friendship.FriendStatus;
import com.beanchiuling.module.user.UserRepository;
import com.beanchiuling.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final DirectConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Transactional
    public FriendDto sendFriendRequest(String requesterId, String addresseeUsername) {
        User addressee = userRepository.findByUsername(addresseeUsername)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (requesterId.equals(addressee.getId())) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_SELF);
        }

        friendshipRepository.findBetweenUsers(requesterId, addressee.getId())
                .ifPresent(f -> { throw new AppException(ErrorCode.FRIEND_ALREADY_EXISTS); });

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendStatus.PENDING)
                .build();

        return toDto(friendshipRepository.save(friendship), requesterId);
    }

    @Transactional
    public FriendDto acceptFriendRequest(String friendshipId, String userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_NOT_FOUND));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN);
        }
        if (friendship.getStatus() != FriendStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_ALREADY_EXISTS);
        }

        friendship.setStatus(FriendStatus.ACCEPTED);
        friendship.setUpdatedAt(Instant.now());

        // Tự tạo DM conversation khi accept
        openOrGetDmConversation(friendship.getRequester().getId(), userId);

        return toDto(friendshipRepository.save(friendship), userId);
    }

    @Transactional
    public void removeFriend(String friendshipId, String userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_NOT_FOUND));

        boolean isParty = friendship.getRequester().getId().equals(userId)
                || friendship.getAddressee().getId().equals(userId);
        if (!isParty) throw new AppException(ErrorCode.SERVER_FORBIDDEN);

        friendshipRepository.delete(friendship);
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getFriends(String userId) {
        return friendshipRepository.findAllByUserIdAndStatus(userId, FriendStatus.ACCEPTED)
                .stream().map(f -> toDto(f, userId)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> getPendingRequests(String userId) {
        return friendshipRepository.findAllByUserIdAndStatus(userId, FriendStatus.PENDING)
                .stream().map(f -> toDto(f, userId)).collect(Collectors.toList());
    }

    @Transactional
    public DirectConversation openOrGetDmConversation(String userAId, String userBId) {
        return conversationRepository.findBetweenUsers(userAId, userBId)
                .orElseGet(() -> {
                    User a = userRepository.findById(userAId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                    User b = userRepository.findById(userBId)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

                    return conversationRepository.save(DirectConversation.builder()
                            .user1(a).user2(b).build());
                });
    }

    private FriendDto toDto(Friendship f, String currentUserId) {
        boolean incoming = f.getAddressee().getId().equals(currentUserId);
        User friend = incoming ? f.getRequester() : f.getAddressee();
        return FriendDto.builder()
                .friendshipId(f.getId())
                .userId(friend.getId())
                .username(friend.getUsernameValue())
                .displayName(friend.getDisplayName())
                .avatarUrl(friend.getAvatarUrl())
                .status(f.getStatus().name())
                .incoming(incoming)
                .since(f.getCreatedAt())
                .build();
    }
}
