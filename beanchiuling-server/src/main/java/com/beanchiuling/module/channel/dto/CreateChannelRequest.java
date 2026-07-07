package com.beanchiuling.module.channel.dto;

import com.beanchiuling.module.channel.entity.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateChannelRequest {

    @NotBlank(message = "Channel name is required")
    @Size(min = 1, max = 100)
    private String name;

    private Channel.ChannelType type = Channel.ChannelType.TEXT;

    @Size(max = 1024)
    private String topic;

    private String categoryId;

    private boolean isPrivate = false;
}
