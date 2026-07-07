package com.beanchiuling.module.channel.dto;

import com.beanchiuling.module.channel.entity.Channel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter @Builder
public class ChannelDto {
    private String id;
    private String serverId;
    private String categoryId;
    private String name;
    private Channel.ChannelType type;
    private String topic;
    private boolean isPrivate;
    private int position;
    private int slowModeDelay;
    private Instant createdAt;
}

