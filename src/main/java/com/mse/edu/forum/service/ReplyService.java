package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.ReplyEntity;
import com.mse.edu.forum.domain.UserEntity;
import com.mse.edu.forum.mapper.ReplyMapper;
import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.repo.ReplyRepository;
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
	public List<ReplyResponse> findByPostId(Long postId) {
		if (!postRepository.existsById(postId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
		}
		return replyRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
				.map(replyMapper::toResponse)
				.toList();
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

	private long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof ForumUserDetails user)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return user.getId();
	}
}
