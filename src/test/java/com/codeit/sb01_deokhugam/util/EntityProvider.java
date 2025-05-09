package com.codeit.sb01_deokhugam.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.codeit.sb01_deokhugam.domain.book.dto.PopularBookDto;
import com.codeit.sb01_deokhugam.domain.book.entity.Book;
import com.codeit.sb01_deokhugam.domain.book.entity.BookRanking;
import com.codeit.sb01_deokhugam.domain.review.entity.Review;
import com.codeit.sb01_deokhugam.domain.user.entity.User;
import com.codeit.sb01_deokhugam.global.enumType.Period;

public class EntityProvider {

	public static User createUser() {
		String randomString = UUID.randomUUID().toString().substring(0, 5);
		return new User(
			"test" + randomString + "@email.com",
			"password486!!",
			"nickname" + randomString);
	}

	public static Book createBook() {
		return new Book(
			"이펙티브 자바",
			"조슈아 블로크",
			"자바 모범 사례를 담은 책입니다.",
			"9780134685991",
			"한빛미디어",
			LocalDate.of(2018, 1, 1),
			"https://example.com/thumbnail.jpg",
			10,
			new BigDecimal("4.8"),
			false
		);
	}

	public static BookRanking createBookRanking(Period period, int rank, BigDecimal score, int reviewCount,
		BigDecimal rating, UUID bookId) {
		return new BookRanking(
			period, rank, score, reviewCount, rating, "https://example.com/thumbnail.jpg",
			"책 제목", "작가", bookId
		);
	}

	public static PopularBookDto createPopularBookDto(UUID bookId, Period period, int rank, Double score,
		int reviewCount, BigDecimal rating) {
		return new PopularBookDto(
			UUID.randomUUID(), bookId, "제목", "작가", "test.com", period, rank, score, reviewCount, rating, Instant.now()
		);
	}

	public static Review createReview(User user, Book book) {
		return new Review(user, book, "좋은 책입니다.", new BigDecimal("4.5"));
	}

	public static Review createReviewByRating(User user, Book book, BigDecimal rating) {
		return new Review(user, book, "좋은 책입니다.", rating);
	}
}
