package com.mse.edu.forum.repo.custom;

import com.mse.edu.forum.domain.PostEntity;

import java.util.List;

public interface PostRepositoryCustom {
    List<PostEntity> findPostsByUserId(Long userId);
}
