package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.mapper.PostMapper;
import com.mse.edu.forum.repo.PostRepository;
import java.util.List;
import java.util.Optional;

import com.mse.edu.forum.repo.custom.PostRepositoryCustom;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

	private final PostRepository postRepository;
    private final PostRepositoryCustom postRepositoryCustom;
	private final PostMapper postMapper;

	public PostService(PostRepository postRepository, PostRepositoryCustom postRepositoryCustom, PostMapper postMapper) {
		this.postRepository = postRepository;
        this.postRepositoryCustom = postRepositoryCustom;
		this.postMapper = postMapper;
	}

	@Transactional(readOnly = true)
	public List<PostResponse> findAll() {
//		return entityListToResponse(postRepository.findAll());
//      return entityListToResponse(postRepository.findAll(PageRequest.of(0,2)).getContent());
      return entityListToResponse(postRepository.findByUserId((long)2));
//      return entityListToResponse(postRepository.whatDoesThisQueryFindByUserId((long)2));
//      return entityListToResponse(postRepositoryCustom.findPostsByUserId((long)2));
	}

	@Transactional(readOnly = true)
	public Optional<PostResponse> findById(Long id) {
		return postRepository.findById(id).map(postMapper::toResponse);
	}

	@Transactional
	public PostResponse create(CreatePostRequest request) {
		PostEntity postEntity = postMapper.toEntity(request);
		PostEntity saved = postRepository.save(postEntity);
		return postMapper.toResponse(saved);
	}

    @Transactional(readOnly = true)
    public List<PostResponse> findByUserId() {
        return postRepository.findAll().stream().map(postMapper::toResponse).toList();
    }

    private List<PostResponse> entityListToResponse(List<PostEntity> entities){
        return entities.stream().map(postMapper::toResponse).toList();
    }
}
