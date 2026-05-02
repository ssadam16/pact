package com.technokratos.pact.friendship.repository;

import com.technokratos.pact.friendship.model.Friendship;
import com.technokratos.pact.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("""
           SELECT CASE WHEN f.who.id = :userId THEN f.toWhom.id ELSE f.who.id END
           FROM Friendship f
           WHERE (f.who.id = :userId OR f.toWhom.id = :userId)
           AND f.status = :status
           """)
    Set<UUID> findFriendIdsByUserIdAndStatus(@Param("userId") UUID userId,
                                             @Param("status") Friendship.FriendshipStatus status);

    @Query("""
        SELECT f FROM Friendship f 
        WHERE (f.who.id = :userId1 AND f.toWhom.id = :userId2)
           OR (f.who.id = :userId2 AND f.toWhom.id = :userId1)
        """)
    Optional<Friendship> findFriendshipBetweenUsers(@Param("userId1") UUID userId1,
                                                    @Param("userId2") UUID userId2);

    @Query("SELECT f FROM Friendship f WHERE f.toWhom.id = :toWhomId AND f.status = :status")
    Set<Friendship> findFriendshipByToWhomAndStatus(@Param("toWhomId") UUID toWhomId,
                                                    @Param("status") Friendship.FriendshipStatus status);
}
