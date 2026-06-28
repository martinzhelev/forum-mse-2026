package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.domain.ReplyEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReplyMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "post", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "content", source = "request.content", qualifiedByName = "trimmed")
	ReplyEntity toEntity(CreateReplyRequest request);

	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "postId", source = "post.id")
	ReplyResponse toResponse(ReplyEntity entity);

	@Named("trimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("instantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}
}
