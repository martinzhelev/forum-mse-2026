package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.api.generated.model.UpdatePostRequest;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.domain.UserRole;
import com.mse.edu.forum.mapper.PostMapper;
import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.ForumUserDetails;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PostService {

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final PostMapper postMapper;

	public PostService(PostRepository postRepository, UserRepository userRepository, PostMapper postMapper) {
		this.postRepository = postRepository;
		this.userRepository = userRepository;
		this.postMapper = postMapper;
	}

	@Transactional(readOnly = true)
	public List<PostResponse> findAll() {
		return entityListToResponse(postRepository.findAllByOrderByIdAsc());
	}

	@Transactional
	public Optional<PostResponse> findByIdAndIncrementViewCount(Long id) {
		return postRepository.findById(id).map(entity -> {
			entity.setViewCount(entity.getViewCount() + 1);
			return postMapper.toResponse(postRepository.save(entity));
		});
	}

	@Transactional
	public PostResponse create(CreatePostRequest request) {
		PostEntity postEntity = postMapper.toEntity(request);
		if (postRepository.existsByTitle(postEntity.getTitle())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic title already in use");
		}
		UserEntity user = userRepository
				.findById(currentUser().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
		postEntity.setUser(user);
		PostEntity saved = postRepository.save(postEntity);
		return postMapper.toResponse(saved);
	}

	@Transactional
	public Optional<PostResponse> update(Long id, UpdatePostRequest request) {
		Optional<PostEntity> existing = postRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		PostEntity entity = existing.get();
		if (!canEdit(entity.getUser().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this topic");
		}
		String newTitle = postMapper.trimmed(request.getTitle());
		if (postRepository.existsByTitleAndIdNot(newTitle, id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Topic title already in use");
		}
		postMapper.applyUpdate(request, entity);
		return Optional.of(postMapper.toResponse(postRepository.save(entity)));
	}

	private ForumUserDetails currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof ForumUserDetails user)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return user;
	}

	private boolean canEdit(long ownerId) {
		ForumUserDetails user = currentUser();
		UserRole role = user.getDomainRole();
		return user.getId() == ownerId || role == UserRole.ADMIN || role == UserRole.MODERATOR;
	}

	private List<PostResponse> entityListToResponse(List<PostEntity> entities) {
		return entities.stream().map(postMapper::toResponse).toList();
	}
}
