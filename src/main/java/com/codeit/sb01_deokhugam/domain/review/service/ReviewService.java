package com.codeit.sb01_deokhugam.domain.review.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.sb01_deokhugam.domain.book.entity.Book;
import com.codeit.sb01_deokhugam.domain.book.exception.BookNotFoundException;
import com.codeit.sb01_deokhugam.domain.book.repository.BookRepository;
import com.codeit.sb01_deokhugam.domain.notification.entity.Notification;
import com.codeit.sb01_deokhugam.domain.notification.repository.NotificationRepository;
import com.codeit.sb01_deokhugam.domain.review.dto.CursorPageResponsePopularReviewDto;
import com.codeit.sb01_deokhugam.domain.review.dto.PopularReviewDto;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewCreateRequest;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewDto;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewLikeDto;
import com.codeit.sb01_deokhugam.domain.review.dto.ReviewUpdateRequest;
import com.codeit.sb01_deokhugam.domain.review.entity.Review;
import com.codeit.sb01_deokhugam.domain.review.entity.ReviewLike;
import com.codeit.sb01_deokhugam.domain.review.entity.ReviewRanking;
import com.codeit.sb01_deokhugam.domain.review.exception.ReviewAlreadyDeletedException;
import com.codeit.sb01_deokhugam.domain.review.exception.ReviewAlreadyExistsException;
import com.codeit.sb01_deokhugam.domain.review.exception.ReviewAuthorityException;
import com.codeit.sb01_deokhugam.domain.review.exception.ReviewNotFoundException;
import com.codeit.sb01_deokhugam.domain.review.mapper.ReviewLikeMapper;
import com.codeit.sb01_deokhugam.domain.review.mapper.ReviewMapper;
import com.codeit.sb01_deokhugam.domain.review.repository.PopularReviewRepository;
import com.codeit.sb01_deokhugam.domain.review.repository.ReviewLikeRepository;
import com.codeit.sb01_deokhugam.domain.review.repository.ReviewRepository;
import com.codeit.sb01_deokhugam.domain.user.entity.User;
import com.codeit.sb01_deokhugam.domain.user.exception.UserNotFoundException;
import com.codeit.sb01_deokhugam.domain.user.repository.UserRepository;
import com.codeit.sb01_deokhugam.global.dto.response.PageResponse;
import com.codeit.sb01_deokhugam.global.enumType.Period;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final BookRepository bookRepository;
	private final UserRepository userRepository;
	private final ReviewLikeRepository reviewLikeRepository;
	private final PopularReviewRepository popularReviewRepository;
	private final ReviewMapper reviewMapper;
	private final ReviewLikeMapper reviewLikeMapper;
	private final NotificationRepository notificationRepository;

	@Transactional
	public ReviewDto createReview(ReviewCreateRequest request) {
		// 1) 작성자, 도서 조회
		User author = userRepository.findByIdAndIsDeletedFalse(request.userId())
			.orElseThrow(() -> UserNotFoundException.withId(request.userId()));
		Book book = bookRepository.findByIdNotLogicalDelete(request.bookId())
			.orElseThrow(() -> new BookNotFoundException().withId(request.bookId()));

		// 2) 활성 리뷰가 이미 있는지 확인 → 있으면 에러
		reviewRepository
			.findByAuthorIdAndBookIdAndDeletedFalse(request.userId(), request.bookId())
			.ifPresent(r -> {
				log.debug("이미 리뷰가 존재합니다. - 사용자 ID: {}, 도서 ID: {}", author.getId(), book.getId());
				throw new ReviewAlreadyExistsException();
			});

		// 4) 새 리뷰 생성
		Review review = new Review(
			author,
			book,
			request.content(),
			request.rating()
		);
		review = reviewRepository.save(review);

		// 5) atomic recalc: COUNT/AVG 재계산
		bookRepository.recalcStats(book.getId());

		// 5) DTO 변환 및 반환
		return reviewMapper.toDto(review);
	}

	@Transactional(readOnly = true)
	public ReviewDto getReview(UUID reviewId, UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw UserNotFoundException.withId(userId);
		}

		Review review = reviewRepository
			.findByIdNotDeleted(reviewId)
			.orElseThrow(() -> new ReviewNotFoundException());

		return reviewMapper.toDto(review);
	}

	@Transactional(readOnly = true)
	public PageResponse<ReviewDto> searchReviews(
		UUID filterUserId,
		UUID filterBookId,
		String keyword,
		String orderBy,
		String direction,
		String cursor,
		Instant after,
		int limit,
		UUID loginUserId
	) {
		List<Review> fetched = reviewRepository.findListByCursor(
			filterUserId, filterBookId, keyword,
			after, cursor,
			orderBy, direction,
			limit + 1
		);

		boolean hasNext = fetched.size() > limit;
		List<Review> page = hasNext
			? fetched.subList(0, limit)
			: fetched;

		long total = reviewRepository.countByFilter(filterUserId, filterBookId, keyword);

		List<ReviewDto> dtos = page.stream()
			.map(reviewMapper::toDto)
			.toList();

		List<UUID> likedReviewIds = (loginUserId != null && !dtos.isEmpty())
			? reviewLikeRepository.findReviewIdsByUserIdAndReviewIdIn(
			loginUserId,
			dtos.stream()
				.map(dto -> dto.id())
				.toList()
		) : List.of();

		Set<UUID> likedSet = new HashSet<>(likedReviewIds);
		List<ReviewDto> calculatedDtos = dtos.stream()
			.map(dto -> new ReviewDto(
				dto.id(),
				dto.bookId(),
				dto.bookTitle(),
				dto.bookThumbnailUrl(),
				dto.userId(),
				dto.userNickname(),
				dto.content(),
				dto.rating(),
				dto.likeCount(),
				dto.commentCount(),
				likedSet.contains(dto.id()),
				dto.createdAt(),
				dto.updatedAt()
			))
			.toList();

		String nextCursor = null;
		Instant nextAfter = null;
		if (hasNext && !page.isEmpty()) {
			Review last = page.get(page.size() - 1);
			nextCursor = "rating".equals(orderBy)
				? last.getRating().toString()
				: last.getId().toString();
			nextAfter = last.getCreatedAt();
		}

		return new PageResponse<>(
			calculatedDtos,
			nextAfter,
			nextCursor,
			dtos.size(),
			hasNext,
			total
		);
	}

	@Transactional
	public ReviewDto updateReview(
		UUID id,
		ReviewUpdateRequest req,
		UUID userId
	) {
		Review review = reviewRepository.findByIdNotDeleted(id)
			.orElseThrow(() -> new ReviewNotFoundException());
		if (!review.getAuthor().getId().equals(userId)) {
			throw new ReviewAuthorityException();
		}
		review.updateReview(req.content(), req.rating());
		log.info("리뷰 업데이트 성공, ID: {}", review.getId());
		bookRepository.recalcStats(review.getBook().getId());
		return reviewMapper.toDto(review);
	}

	@Transactional
	public void hardDeleteReview(UUID reviewId, UUID userId) {
		Review review =
			reviewRepository
				.findById(reviewId)
				.orElseThrow(() -> new ReviewNotFoundException());

		if (review.isDeleted()) {
			log.debug("이미 삭제된 리뷰입니다.");
			throw new ReviewAlreadyDeletedException();
		}

		if (review.getAuthor().getId().equals(userId)) {
			reviewRepository.delete(review);
		} else {
			log.debug("리뷰 물리 삭제 권한 없습니다. - 사용자 ID: {}", userId);
			throw new ReviewAuthorityException();
		}
		bookRepository.recalcStats(review.getBook().getId());
	}

	@Transactional
	public void softDeleteReview(UUID reviewId, UUID userId) {
		Review review =
			reviewRepository
				.findById(reviewId)
				.orElseThrow(() -> new ReviewNotFoundException());

		if (review.isDeleted()) {
			log.debug("이미 삭제된 리뷰입니다.");
			throw new ReviewAlreadyDeletedException();
		}

		if (!review.getAuthor().getId().equals(userId)) {
			log.debug("리뷰 논리 삭제 권한 없습니다. - 사용자 ID: {}", userId);
			throw new ReviewAuthorityException();
		}
		review.delete(); //delete field를 true로
		bookRepository.recalcStats(review.getBook().getId());
	}

	@Transactional
	public ReviewLikeDto likeReview(UUID reviewId, UUID userId) {
		// ── 1) 리뷰 / 유저 검사
		Review review = reviewRepository.findById(reviewId)
			.orElseThrow(() -> new ReviewNotFoundException());
		if (review.isDeleted()) {
			throw new ReviewAlreadyDeletedException();
		}
		if (!userRepository.existsById(userId)) {
			throw new ReviewAuthorityException();
		}

		// ── 2) 기존 좋아요 여부 확인
		Optional<ReviewLike> existing =
			reviewLikeRepository.findByReviewIdAndUserId(reviewId, userId);

		boolean likedAfter;
		ReviewLike rlEntity = null;
		if (existing.isPresent()) {
			// → 이미 좋아요 → 취소
			reviewLikeRepository.deleteByReviewIdAndUserId(reviewId, userId);
			reviewRepository.decrementLikeCount(reviewId);
			likedAfter = false;
			rlEntity = new ReviewLike(reviewId, userId);
		} else {
			// → 신규 좋아요
			rlEntity = reviewLikeRepository.save(new ReviewLike(reviewId, userId));
			reviewRepository.incrementLikeCount(reviewId);
			createNewLikeNotification(userId, review);
			likedAfter = true;
		}

		// ── 3) likedByMe 토글
		//review.setLikedByMe(likedAfter);

		return reviewLikeMapper.toDto(rlEntity, likedAfter);
	}

	private void createNewLikeNotification(UUID userId, Review review) {
		User findUser = userRepository.findById(userId).orElseThrow(() -> UserNotFoundException.withId(userId));
		Notification notification = Notification.fromLike(findUser, review);
		notificationRepository.save(notification);
	}

	@Transactional(readOnly = true)
	public CursorPageResponsePopularReviewDto getPopularReviews(
		Period period,
		String direction,
		String cursor,
		Instant after,
		Integer limit
	) {
		int pageSize = (limit != null && limit > 0) ? limit : 20;

		// 1) limit+1 개를 가져와서 hasNext 판단
		List<ReviewRanking> fetched = popularReviewRepository
			.findByPeriodWithCursor(period, cursor, after, pageSize + 1);
		boolean hasNext = fetched.size() > pageSize;
		List<ReviewRanking> page = hasNext
			? fetched.subList(0, pageSize)
			: fetched;

		// 2) 전체 카운트 (페이징 UI 용)
		long total = popularReviewRepository.countByPeriod(period);

		// 3) 엔티티 → DTO 변환
		List<PopularReviewDto> dtos = page.stream().map(r -> new PopularReviewDto(
			r.getId(),
			r.getReview().getId(),
			r.getBookId(),
			r.getBookTitle(),
			r.getBookThumbnailUrl(),
			r.getUserId(),
			r.getUserNickname(),
			r.getReviewRating(),
			r.getPeriod(),
			r.getRank(),
			r.getScore(),
			r.getLikeCount(),
			r.getCommentCount(),
			r.getCreatedAt()
		)).toList();

		// 4) nextCursor/nextAfter
		String nextCursorRes = null;
		Instant nextAfterRes = null;
		if (hasNext && !page.isEmpty()) {
			ReviewRanking last = page.get(page.size() - 1);
			nextCursorRes = String.valueOf(last.getRank());
			nextAfterRes = last.getCreatedAt();
		}

		// 5) 커서페이지 형식으로 반환
		return new CursorPageResponsePopularReviewDto(
			dtos,
			nextCursorRes,
			nextAfterRes,
			pageSize,
			hasNext,
			total
		);
	}
}
