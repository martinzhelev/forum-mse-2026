package com.mse.edu.forum.repo.custom.impl;

import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.repo.custom.PostRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostRepositoryCustomImpl implements PostRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<PostEntity> findPostsByUserId(Long userId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<PostEntity> cq = cb.createQuery(PostEntity.class);
        Root<PostEntity> post = cq.from(PostEntity.class);

        cq.select(post)
                .where(cb.equal(post.get("user").get("id"), userId));

        return entityManager.createQuery(cq).getResultList();
    }
}
