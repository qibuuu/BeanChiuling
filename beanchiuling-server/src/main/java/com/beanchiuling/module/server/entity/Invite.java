package com.beanchiuling.module.server.entity;

import com.beanchiuling.module.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "invites")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invite {

    @Id
    @Column(name = "code", length = 10)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    @Builder.Default
    private int currentUses = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
