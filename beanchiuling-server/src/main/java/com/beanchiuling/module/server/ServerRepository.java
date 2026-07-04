package com.beanchiuling.module.server;

import com.beanchiuling.module.server.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, String> {

    Optional<Server> findByInviteCode(String inviteCode);

    @Query("SELECT s FROM Server s JOIN s.members m WHERE m.user.id = :userId")
    List<Server> findAllByMemberUserId(@Param("userId") String userId);

    boolean existsByInviteCode(String inviteCode);
}
