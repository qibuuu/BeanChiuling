package com.beanchiuling.module.server;

import com.beanchiuling.module.server.entity.ServerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerMemberRepository extends JpaRepository<ServerMember, String> {

    Optional<ServerMember> findByUserIdAndServerId(String userId, String serverId);

    List<ServerMember> findAllByServerId(String serverId);

    boolean existsByUserIdAndServerId(String userId, String serverId);

    long countByServerId(String serverId);

    void deleteByUserIdAndServerId(String userId, String serverId);
}
