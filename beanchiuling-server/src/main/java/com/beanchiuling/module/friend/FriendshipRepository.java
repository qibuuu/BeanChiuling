package com.beanchiuling.module.friend;

import com.beanchiuling.module.friend.entity.Friendship;
import com.beanchiuling.module.friend.entity.Friendship.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, String> {

    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = :uid OR f.addressee.id = :uid) AND f.status = :status")
    List<Friendship> findAllByUserIdAndStatus(@Param("uid") String userId, @Param("status") FriendStatus status);

    @Query("SELECT f FROM Friendship f WHERE (f.requester.id = :a AND f.addressee.id = :b) OR (f.requester.id = :b AND f.addressee.id = :a)")
    Optional<Friendship> findBetweenUsers(@Param("a") String userAId, @Param("b") String userBId);

    @Query("SELECT COUNT(f) > 0 FROM Friendship f WHERE ((f.requester.id = :a AND f.addressee.id = :b) OR (f.requester.id = :b AND f.addressee.id = :a)) AND f.status = :status")
    boolean existsBetweenUsersWithStatus(@Param("a") String userAId, @Param("b") String userBId, @Param("status") FriendStatus status);
}
