package com.beanchiuling.module.channel;

import com.beanchiuling.module.channel.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, String> {

    List<Channel> findAllByServerIdOrderByPositionAsc(String serverId);

    List<Channel> findAllByCategoryIdOrderByPositionAsc(String categoryId);

    Optional<Channel> findByIdAndServerId(String id, String serverId);

    boolean existsByNameAndServerId(String name, String serverId);

    long countByServerId(String serverId);
}
