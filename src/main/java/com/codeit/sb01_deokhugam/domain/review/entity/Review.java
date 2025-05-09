package com.codeit.sb01_deokhugam.domain.review.entity;

import java.math.BigDecimal;

import com.codeit.sb01_deokhugam.domain.base.BaseUpdatableEntity;
import com.codeit.sb01_deokhugam.domain.book.entity.Book;
import com.codeit.sb01_deokhugam.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseUpdatableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User author;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "rating", nullable = false, precision = 2, scale = 1)
	private BigDecimal rating;

	@Column(name = "like_count", nullable = false)
	private Integer likeCount = 0;

	@Column(name = "comment_count", nullable = false)
	private Integer commentCount = 0;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted = false;

	public Review(User author, Book book, String content, BigDecimal rating) {
		this.author = author;
		this.book = book;
		this.content = content;
		this.rating = rating;
	}

	public void delete() {
		this.deleted = true;
	}

	public void incrementCommentCount() {
		this.commentCount++;
	}

	public void decrementCommentCount() {
		if (this.commentCount > 0) {
			this.commentCount--;
		}
	}

	public void updateReview(String content, BigDecimal rating) {
		this.content = content;
		this.rating = rating;
	}
}
