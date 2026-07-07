package com.beanchiuling.module.message.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class WsEvent {
    private String type;
    private Object data;

    public static final String MESSAGE_CREATE = "MESSAGE_CREATE";
    public static final String MESSAGE_UPDATE = "MESSAGE_UPDATE";
    public static final String MESSAGE_DELETE = "MESSAGE_DELETE";
    public static final String TYPING_START   = "TYPING_START";
    public static final String TYPING_STOP    = "TYPING_STOP";
    public static final String PRESENCE_UPDATE = "PRESENCE_UPDATE";
}
