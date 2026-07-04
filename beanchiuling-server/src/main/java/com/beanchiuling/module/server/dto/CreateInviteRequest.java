package com.beanchiuling.module.server.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInviteRequest {

    @Min(value = 1, message = "Max uses must be at least 1")
    private Integer maxUses;

    private Integer expiresInHours;
}
