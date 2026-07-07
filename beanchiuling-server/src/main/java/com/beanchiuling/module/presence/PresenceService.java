package com.beanchiuling.module.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String PRESENCE_KEY_PREFIX = "presence:";
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(35);

    public void setOnline(String userId, String status) {
        String key = PRESENCE_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, status, PRESENCE_TTL);
        log.debug("User {} is now {}", userId, status);
    }

    public void setOffline(String userId) {
        redisTemplate.delete(PRESENCE_KEY_PREFIX + userId);
        log.debug("User {} went offline", userId);
    }

    public String getStatus(String userId) {
        String val = redisTemplate.opsForValue().get(PRESENCE_KEY_PREFIX + userId);
        return val != null ? val : "offline";
    }

    public Map<String, String> getBulkStatus(List<String> userIds) {
        return userIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::getStatus
                ));
    }

    public void heartbeat(String userId) {
        String key = PRESENCE_KEY_PREFIX + userId;
        String current = redisTemplate.opsForValue().get(key);
        if (current != null) {
            redisTemplate.expire(key, PRESENCE_TTL);
        }
    }
}
