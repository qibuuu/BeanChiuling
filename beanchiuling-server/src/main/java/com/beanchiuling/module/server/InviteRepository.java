package com.beanchiuling.module.server;

import com.beanchiuling.module.server.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepository extends JpaRepository<Invite, String> {

    Optional<Invite> findByCode(String code);

    void deleteByCode(String code);
}
