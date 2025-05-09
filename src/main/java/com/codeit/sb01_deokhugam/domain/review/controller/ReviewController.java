package com.codeit.sb01_deokhugam.domain.review.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeit.sb01_deokhugam.domain.review.dto.CursorPageResponsePopularReviewDto;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewCreateRequest;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewDto;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewLikeDto;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewUpdateRequest;
import com.codeit.sb01_deokhugam.domain.review.service.ReviewService;
import com.codeit.sb01_deokhugam.global.dto.response.PageResponse;
import com.codeit.sb01_deokhugam.global.enumType.Period;
import com.codeit.sb01_deokhugam.global.resolver.annotation.LoginUserId;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("api/reviews")
@RequiredArgsConstructor
public class ReviewController {
	private final ReviewService reviewService;

	@PostMapping
	public ResponseEntity<ReviewDto> createReview(
		@Valid @RequestBody ReviewCreateRequest request
	) {
		log.info("리뷰 생성 요청 : {}", request);
		ReviewDto created = reviewService.createReview(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@PostMapping("/{reviewId}/like")
	public ResponseEntity<ReviewLikeDto> likeReview(
		@PathVariable("reviewId") UUID reviewId,
		@LoginUserId UUID userId
	) {
		log.info("리뷰 좋아요 요청 : {}", reviewId);
		ReviewLikeDto result = reviewService.likeReview(reviewId, userId);
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ReviewDto> getReview(
		@PathVariable UUID id,
		@LoginUserId UUID userId
	) {
		ReviewDto reveiw = reviewService.getReview(id, userId);
		return ResponseEntity.ok(reveiw);
	}

	@GetMapping
	public ResponseEntity<PageResponse<ReviewDto>> getReviews(
		@RequestParam(name = "userId", required = false) UUID userId,
		@RequestParam(name = "bookId", required = false) UUID bookId,
		@RequestParam(name = "keyword", required = false) String keyword,
		@RequestParam(name = "orderBy", defaultValue = "createdAt") String orderBy,
		@RequestParam(name = "direction", defaultValue = "DESC") String direction,
		@RequestParam(name = "cursor", required = false) String cursor,
		@RequestParam(name = "after", required = false) Instant after,
		@RequestParam(name = "limit", defaultValue = "50") int limit,
		@LoginUserId UUID loginUserId
	) {
		log.info("리뷰 목록 조회 요청 : {}, {}, {}", keyword, orderBy, direction);
		PageResponse<ReviewDto> page = reviewService.searchReviews(
			userId,
			bookId,
			keyword,
			orderBy,
			direction,
			cursor,
			after,
			limit,
			loginUserId
		);
		return ResponseEntity.ok(page);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<ReviewDto> updateReview(
		@PathVariable UUID id,
		@Valid @RequestBody ReviewUpdateRequest request,
		@LoginUserId UUID userId
	) {
		log.info("리뷰 수정 요청 : {}", id);
		ReviewDto updated = reviewService.updateReview(id, request, userId);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> softDeleteReview(
		@PathVariable UUID id,
		@LoginUserId UUID userId
	) {
		reviewService.softDeleteReview(id, userId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}/hard")
	public ResponseEntity<Void> hardDeleteReview(
		@PathVariable UUID id,
		@LoginUserId UUID userId
	) {
		reviewService.hardDeleteReview(id, userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/popular")
	public ResponseEntity<CursorPageResponsePopularReviewDto> getPopularReviews(
		@RequestParam(name = "period") Period period,
		@RequestParam(name = "direction", defaultValue = "ASC") String direction,
		@RequestParam(name = "cursor", required = false) String cursor,
		@RequestParam(name = "after", required = false) Instant after,
		@RequestParam(name = "limit", required = false) Integer limit
	) {
		log.info("인기 리뷰 목록 조회 요청 : {}, {}", period, direction);
		CursorPageResponsePopularReviewDto result =
			reviewService.getPopularReviews(period, direction, cursor, after, limit);
		return ResponseEntity.ok(result);
	}
}
