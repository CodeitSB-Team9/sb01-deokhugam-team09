package com.codeit.sb01_deokhugam.domain.review.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.codeit.sb01_deokhugam.domain.review.dto.ReviewDto;
import com.codeit.sb01_deokhugam.domain.review.entity.Review;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
	@Mapping(target = "bookId", source = "review.book.id")
	@Mapping(target = "bookTitle", source = "review.book.title")
	@Mapping(target = "bookThumbnailUrl", source = "review.book.thumbnailUrl")
	@Mapping(target = "userId", source = "review.author.id")
	@Mapping(target = "userNickname", source = "review.author.nickname")
	ReviewDto toDto(Review review);
}
