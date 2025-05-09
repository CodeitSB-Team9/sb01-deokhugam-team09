package com.codeit.sb01_deokhugam.domain.review.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.codeit.sb01_deokhugam.domain.base.BaseEntity;
import com.codeit.sb01_deokhugam.domain.review.entity.Review;
import com.codeit.sb01_deokhugam.global.enumType.Period;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
	name = "review_rankings",
	indexes = {
		@Index(name = "idx_review_rankings_period_rank", columnList = "period, rank"),
		@Index(name = "idx_review_rankings_period_created_at", columnList = "period, created_at")
	}
)
public class ReviewRanking extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
		name = "review_id",
		nullable = false,
		foreignKey = @ForeignKey(name = "fk_review_ranking_review")
	)
	private Review review;

	@Column(name = "book_id", nullable = false, columnDefinition = "uuid")
	private UUID bookId;

	@Column(name = "book_title", nullable = false)
	private String bookTitle;

	@Column(name = "book_thumbnail_url", nullable = false)
	private String bookThumbnailUrl;

	@Column(name = "user_id", nullable = false, columnDefinition = "uuid")
	private UUID userId;

	@Column(name = "user_nickname", nullable = false)
	private String userNickname;

	@Column(name = "review_rating", nullable = false, precision = 2, scale = 1)
	private BigDecimal reviewRating;

	@Enumerated(EnumType.STRING)
	@Column(name = "period", nullable = false)
	private Period period;

	@Column(name = "rank", nullable = false)
	private Integer rank;

	@Column(name = "score", nullable = false, precision = 10, scale = 2)
	private BigDecimal score;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount;

	@Column(name = "comment_count", nullable = false)
	private Integer commentCount;
}
