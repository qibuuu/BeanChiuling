package com.beanchiuling.module.server;

import com.beanchiuling.common.exception.AppException;
import com.beanchiuling.common.exception.ErrorCode;
import com.beanchiuling.common.utils.PermissionUtils;
import com.beanchiuling.module.server.dto.*;
import com.beanchiuling.module.server.entity.*;
import com.beanchiuling.module.user.UserRepository;
import com.beanchiuling.module.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerService {

    private final ServerRepository serverRepository;
    private final ServerMemberRepository memberRepository;
    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;

    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ServerDetailDto createServer(String userId, CreateServerRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Server server = Server.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .isPublic(request.isPublic())
                .inviteCode(generateUniqueInviteCode())
                .build();

        server = serverRepository.save(server);

        Role everyoneRole = Role.builder()
                .server(server)
                .name("@everyone")
                .permissions(PermissionUtils.DEFAULT_PERMISSIONS)
                .position(0)
                .build();

        server.getRoles().add(everyoneRole);

        ServerMember ownerMember = ServerMember.builder()
                .user(owner)
                .server(server)
                .build();

        ownerMember.getRoles().add(everyoneRole);
        memberRepository.save(ownerMember);

        log.info("Server created: '{}' by user {}", server.getName(), userId);
        return toDetailDto(server, List.of(ownerMember));
    }

    @Transactional(readOnly = true)
    public ServerDetailDto getServer(String serverId, String requestingUserId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (!memberRepository.existsByUserIdAndServerId(requestingUserId, serverId)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN);
        }

        List<ServerMember> members = memberRepository.findAllByServerId(serverId);
        return toDetailDto(server, members);
    }

    @Transactional(readOnly = true)
    public List<ServerDetailDto> getUserServers(String userId) {
        List<Server> servers = serverRepository.findAllByMemberUserId(userId);
        return servers.stream()
                .map(s -> toDetailDto(s, List.of()))
                .collect(Collectors.toList());
    }

    @Transactional
    public ServerDetailDto joinByInviteCode(String code, String userId) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.INVITE_NOT_FOUND));

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.INVITE_EXPIRED);
        }

        if (invite.getMaxUses() != null && invite.getCurrentUses() >= invite.getMaxUses()) {
            throw new AppException(ErrorCode.INVITE_MAX_USES_REACHED);
        }

        Server server = invite.getServer();

        if (memberRepository.existsByUserIdAndServerId(userId, server.getId())) {
            throw new AppException(ErrorCode.SERVER_ALREADY_MEMBER);
        }

        long currentCount = memberRepository.countByServerId(server.getId());
        if (currentCount >= server.getMaxMembers()) {
            throw new AppException(ErrorCode.SERVER_FULL);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Role everyoneRole = server.getRoles().stream()
                .filter(r -> r.getName().equals("@everyone"))
                .findFirst()
                .orElseThrow();

        ServerMember newMember = ServerMember.builder()
                .user(user)
                .server(server)
                .build();
        newMember.getRoles().add(everyoneRole);
        memberRepository.save(newMember);

        invite.setCurrentUses(invite.getCurrentUses() + 1);
        inviteRepository.save(invite);

        log.info("User {} joined server '{}' via invite {}", userId, server.getName(), code);
        return toDetailDto(server, memberRepository.findAllByServerId(server.getId()));
    }

    @Transactional
    public void leaveServer(String serverId, String userId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (server.getOwner().getId().equals(userId)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN,
                    "Server owner cannot leave. Transfer ownership or delete the server.");
        }

        memberRepository.deleteByUserIdAndServerId(userId, serverId);
        log.info("User {} left server {}", userId, serverId);
    }

    @Transactional
    public void kickMember(String serverId, String targetUserId, String requestingUserId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        long requesterPerms = resolvePermissions(requestingUserId, server);
        if (!PermissionUtils.has(requesterPerms, PermissionUtils.KICK_MEMBERS)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN);
        }

        if (server.getOwner().getId().equals(targetUserId)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN, "Cannot kick the server owner");
        }

        memberRepository.deleteByUserIdAndServerId(targetUserId, serverId);
        log.info("User {} kicked from server {} by {}", targetUserId, serverId, requestingUserId);
    }

    @Transactional
    public void deleteServer(String serverId, String requestingUserId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        if (!server.getOwner().getId().equals(requestingUserId)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN, "Only the server owner can delete it");
        }

        serverRepository.delete(server);
        log.info("Server {} deleted by owner {}", serverId, requestingUserId);
    }

    @Transactional
    public InviteDto createInvite(String serverId, String userId, CreateInviteRequest request) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        long perms = resolvePermissions(userId, server);
        if (!PermissionUtils.has(perms, PermissionUtils.MANAGE_SERVER)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN);
        }

        User inviter = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Instant now = Instant.now();
        Instant expiresAt = request.getExpiresInHours() != null
                ? now.plusSeconds(request.getExpiresInHours() * 3600L)
                : null;

        Invite invite = Invite.builder()
                .code(generateUniqueInviteCode())
                .server(server)
                .inviter(inviter)
                .maxUses(request.getMaxUses())
                .expiresAt(expiresAt)
                .createdAt(now)
                .build();

        inviteRepository.save(invite);
        return toInviteDto(invite, (int) memberRepository.countByServerId(serverId));
    }

    @Transactional(readOnly = true)
    public InviteDto getInvite(String code) {
        Invite invite = inviteRepository.findByCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.INVITE_NOT_FOUND));

        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.INVITE_EXPIRED);
        }

        int memberCount = (int) memberRepository.countByServerId(invite.getServer().getId());
        return toInviteDto(invite, memberCount);
    }

    private long resolvePermissions(String userId, Server server) {
        if (server.getOwner().getId().equals(userId)) {
            return PermissionUtils.ADMINISTRATOR;
        }

        return memberRepository.findByUserIdAndServerId(userId, server.getId())
                .map(member -> member.getRoles().stream()
                        .mapToLong(Role::getPermissions)
                        .reduce(0L, (a, b) -> a | b))
                .orElse(0L);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (serverRepository.existsByInviteCode(code));
        return code;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private ServerDetailDto toDetailDto(Server server, List<ServerMember> members) {
        return ServerDetailDto.builder()
                .id(server.getId())
                .name(server.getName())
                .description(server.getDescription())
                .iconUrl(server.getIconUrl())
                .bannerUrl(server.getBannerUrl())
                .inviteCode(server.getInviteCode())
                .isPublic(server.isPublic())
                .maxMembers(server.getMaxMembers())
                .memberCount(members.size())
                .createdAt(server.getCreatedAt())
                .owner(ServerDetailDto.OwnerInfo.builder()
                        .id(server.getOwner().getId())
                        .username(server.getOwner().getUsernameValue())
                        .displayName(server.getOwner().getDisplayName())
                        .avatarUrl(server.getOwner().getAvatarUrl())
                        .build())
                .members(members.stream().map(m -> ServerDetailDto.MemberInfo.builder()
                        .memberId(m.getId())
                        .userId(m.getUser().getId())
                        .username(m.getUser().getUsernameValue())
                        .displayName(m.getUser().getDisplayName())
                        .avatarUrl(m.getUser().getAvatarUrl())
                        .nickname(m.getNickname())
                        .joinedAt(m.getJoinedAt())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private InviteDto toInviteDto(Invite invite, int memberCount) {
        return InviteDto.builder()
                .code(invite.getCode())
                .serverId(invite.getServer().getId())
                .serverName(invite.getServer().getName())
                .serverIconUrl(invite.getServer().getIconUrl())
                .memberCount(memberCount)
                .inviterUsername(invite.getInviter().getUsernameValue())
                .maxUses(invite.getMaxUses())
                .currentUses(invite.getCurrentUses())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .build();
    }
}
