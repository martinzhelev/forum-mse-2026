package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyPageResponse;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.api.generated.model.UpdateReplyRequest;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.ReplyEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.domain.UserRole;
import com.mse.edu.forum.mapper.ReplyMapper;
import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.repo.ReplyRepository;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.ForumUserDetails;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReplyService {

	private final ReplyRepository replyRepository;
	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final ReplyMapper replyMapper;

	public ReplyService(
			ReplyRepository replyRepository,
			PostRepository postRepository,
			UserRepository userRepository,
			ReplyMapper replyMapper) {
		this.replyRepository = replyRepository;
		this.postRepository = postRepository;
		this.userRepository = userRepository;
		this.replyMapper = replyMapper;
	}

	@Transactional(readOnly = true)
	public ReplyPageResponse findByPostId(Long postId, Integer page, Integer size) {
		if (!postRepository.existsById(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
		}
		int safePage = page == null ? 0 : Math.max(0, page);
		int safeSize = size == null ? 10 : Math.max(1, Math.min(100, size));
		Page<ReplyEntity> replies = replyRepository.findByPostIdOrderByCreatedAtAsc(
				postId, PageRequest.of(safePage, safeSize));
		return new ReplyPageResponse(
				replies.stream().map(replyMapper::toResponse).toList(),
				replies.getNumber(),
				replies.getSize(),
				replies.getTotalElements(),
				replies.getTotalPages());
	}

	@Transactional(readOnly = true)
	public Optional<ReplyResponse> findById(Long id) {
		return replyRepository.findById(id).map(replyMapper::toResponse);
	}

	@Transactional
	public ReplyResponse create(Long postId, CreateReplyRequest request) {
		PostEntity post = postRepository
				.findById(postId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
		UserEntity user = userRepository
				.findById(currentUserId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

		ReplyEntity entity = replyMapper.toEntity(request);
		entity.setPost(post);
		entity.setUser(user);
		ReplyEntity saved = replyRepository.save(entity);
		return replyMapper.toResponse(saved);
	}

	@Transactional
	public Optional<ReplyResponse> update(Long id, UpdateReplyRequest request) {
		Optional<ReplyEntity> existing = replyRepository.findById(id);
		if (existing.isEmpty()) {
			return Optional.empty();
		}
		ReplyEntity entity = existing.get();
		if (!canEdit(entity.getUser().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this reply");
		}
		replyMapper.applyUpdate(request, entity);
		return Optional.of(replyMapper.toResponse(replyRepository.save(entity)));
	}

	private ForumUserDetails currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof ForumUserDetails user)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return user;
	}

	private long currentUserId() {
		return currentUser().getId();
	}

	private boolean canEdit(long ownerId) {
		ForumUserDetails user = currentUser();
		UserRole role = user.getDomainRole();
		return user.getId() == ownerId || role == UserRole.ADMIN || role == UserRole.MODERATOR;
	}
}
