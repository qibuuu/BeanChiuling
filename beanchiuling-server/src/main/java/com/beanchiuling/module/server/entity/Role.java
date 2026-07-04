package com.beanchiuling.module.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    @Builder.Default
    private long permissions = 0L;

    @Column(name = "is_hoisted", nullable = false)
    @Builder.Default
    private boolean isHoisted = false;

    @Column(name = "is_mentionable", nullable = false)
    @Builder.Default
    private boolean isMentionable = false;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;
}
