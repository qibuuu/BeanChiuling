package com.beanchiuling.module.friend;

import com.beanchiuling.module.friend.entity.DirectConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DirectConversationRepository extends JpaRepository<DirectConversation, String> {

    @Query("SELECT dc FROM DirectConversation dc WHERE dc.user1.id = :uid OR dc.user2.id = :uid ORDER BY dc.lastMessageAt DESC")
    List<DirectConversation> findAllByUserId(@Param("uid") String userId);

    @Query("SELECT dc FROM DirectConversation dc WHERE (dc.user1.id = :a AND dc.user2.id = :b) OR (dc.user1.id = :b AND dc.user2.id = :a)")
    Optional<DirectConversation> findBetweenUsers(@Param("a") String userAId, @Param("b") String userBId);
}
