package com.beanchiuling.module.channel;

import com.beanchiuling.module.channel.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    List<Category> findAllByServerIdOrderByPositionAsc(String serverId);

    boolean existsByNameAndServerId(String name, String serverId);
}
