package com.beanchiuling.module.channel;

import com.beanchiuling.common.exception.AppException;
import com.beanchiuling.common.exception.ErrorCode;
import com.beanchiuling.common.utils.PermissionUtils;
import com.beanchiuling.module.channel.dto.ChannelDto;
import com.beanchiuling.module.channel.dto.CreateChannelRequest;
import com.beanchiuling.module.channel.entity.Category;
import com.beanchiuling.module.channel.entity.Channel;
import com.beanchiuling.module.server.ServerMemberRepository;
import com.beanchiuling.module.server.ServerRepository;
import com.beanchiuling.module.server.entity.Role;
import com.beanchiuling.module.server.entity.Server;
import com.beanchiuling.module.server.entity.ServerMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final CategoryRepository categoryRepository;
    private final ServerRepository serverRepository;
    private final ServerMemberRepository memberRepository;

    @Transactional
    public ChannelDto createChannel(String serverId, String userId, CreateChannelRequest req) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));

        requirePermission(userId, server, PermissionUtils.MANAGE_CHANNELS);

        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));
        }

        int position = (int) channelRepository.countByServerId(serverId);

        Channel channel = Channel.builder()
                .server(server)
                .category(category)
                .name(req.getName().toLowerCase().replace(" ", "-"))
                .type(req.getType())
                .topic(req.getTopic())
                .isPrivate(req.isPrivate())
                .position(position)
                .build();

        return toDto(channelRepository.save(channel));
    }

    @Transactional(readOnly = true)
    public List<ChannelDto> getServerChannels(String serverId, String userId) {
        requireMembership(userId, serverId);
        return channelRepository.findAllByServerIdOrderByPositionAsc(serverId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void deleteChannel(String channelId, String userId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new AppException(ErrorCode.CHANNEL_NOT_FOUND));

        requirePermission(userId, channel.getServer(), PermissionUtils.MANAGE_CHANNELS);
        channelRepository.delete(channel);
    }

    private void requireMembership(String userId, String serverId) {
        if (!memberRepository.existsByUserIdAndServerId(userId, serverId)) {
            throw new AppException(ErrorCode.SERVER_FORBIDDEN);
        }
    }

    private void requirePermission(String userId, Server server, long flag) {
        if (server.getOwner().getId().equals(userId)) return;

        long perms = memberRepository.findByUserIdAndServerId(userId, server.getId())
                .map(m -> m.getRoles().stream()
                        .mapToLong(Role::getPermissions).reduce(0L, (a, b) -> a | b))
                .orElse(0L);

        if (!PermissionUtils.has(perms, flag)) {
            throw new AppException(ErrorCode.CHANNEL_FORBIDDEN);
        }
    }

    private ChannelDto toDto(Channel c) {
        return ChannelDto.builder()
                .id(c.getId())
                .serverId(c.getServer().getId())
                .categoryId(c.getCategory() != null ? c.getCategory().getId() : null)
                .name(c.getName())
                .type(c.getType())
                .topic(c.getTopic())
                .isPrivate(c.isPrivate())
                .position(c.getPosition())
                .slowModeDelay(c.getSlowModeDelay())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
