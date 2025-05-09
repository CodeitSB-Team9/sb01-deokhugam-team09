package com.codeit.sb01_deokhugam.domain.review.entity;

import java.util.UUID;

import com.codeit.sb01_deokhugam.domain.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "review_likes",
	uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id"}),
	indexes = @Index(name = "idx_review_likes_user_review", columnList = "user_id, review_id")
)
public class ReviewLike extends BaseEntity {

	@Column(name = "review_id", nullable = false, columnDefinition = "uuid")
	private UUID reviewId;

	@Column(name = "user_id", nullable = false, columnDefinition = "uuid")
	private UUID userId;

	public ReviewLike(UUID reviewId, UUID userId) {
		this.reviewId = reviewId;
		this.userId = userId;
	}
}
