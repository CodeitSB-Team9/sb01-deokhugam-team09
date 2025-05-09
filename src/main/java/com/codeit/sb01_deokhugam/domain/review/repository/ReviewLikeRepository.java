package com.codeit.sb01_deokhugam.domain.review.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.codeit.sb01_deokhugam.domain.review.entity.ReviewLike;

public interface ReviewLikeRepository
	extends JpaRepository<ReviewLike, UUID> {

	Optional<ReviewLike> findByReviewIdAndUserId(UUID reviewId, UUID userId);

	// 토글용 삭제
	void deleteByReviewIdAndUserId(UUID reviewId, UUID userId);

	List<ReviewLike> findByCreatedAtBetween(Instant start, Instant end);

	@Query("select rl.reviewId from ReviewLike rl "
		+ "where rl.userId = :userId and rl.reviewId in :reviewIds")
	List<UUID> findReviewIdsByUserIdAndReviewIdIn(
		@Param("userId") UUID userId,
		@Param("reviewIds") List<UUID> reviewIds
	);
  
	void deleteByUserId(UUID userId);
}
