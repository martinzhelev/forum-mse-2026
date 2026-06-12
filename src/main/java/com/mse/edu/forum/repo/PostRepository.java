package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findByUserId(Long userId);

    @Query("""
            SELECT post
            FROM PostEntity post
            JOIN post.replies reply
            WHERE post.user.id = :userId
            AND reply.user.id = :userId
            """)
    List<PostEntity> whatDoesThisQueryFindByUserId(Long userId);
}
