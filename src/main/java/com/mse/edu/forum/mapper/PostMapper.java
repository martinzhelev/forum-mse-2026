package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.api.generated.model.UpdatePostRequest;
import com.mse.edu.forum.api.generated.model.UserRole;
import com.mse.edu.forum.api.generated.model.UserSummary;
import com.mse.edu.forum.domain.PostEntity;
import com.mse.edu.forum.domain.UserEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface PostMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "replies", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "trimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	PostEntity toEntity(CreatePostRequest request);

	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "modifiedAt", source = "modifiedAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "author", source = "user", qualifiedByName = "userToSummary")
	PostResponse toResponse(PostEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "modifiedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "user", ignore = true)
	@Mapping(target = "replies", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "trimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	void applyUpdate(UpdatePostRequest request, @MappingTarget PostEntity entity);

	@Named("trimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("instantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}

	@Named("userToSummary")
	default UserSummary userToSummary(UserEntity user) {
		if (user == null) {
			return null;
		}
		return new UserSummary(user.getId(), user.getUsername(), UserRole.valueOf(user.getRole().name()));
	}
}
