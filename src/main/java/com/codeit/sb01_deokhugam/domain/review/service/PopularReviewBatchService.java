package com.codeit.sb01_deokhugam.domain.review.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.sb01_deokhugam.domain.comment.entity.Comment;
import com.codeit.sb01_deokhugam.domain.comment.repository.CommentRepository;
import com.codeit.sb01_deokhugam.domain.notification.entity.Notification;
import com.codeit.sb01_deokhugam.domain.notification.repository.NotificationRepository;
import com.codeit.sb01_deokhugam.domain.review.entity.Review;
import com.codeit.sb01_deokhugam.domain.review.entity.ReviewLike;
import com.codeit.sb01_deokhugam.domain.review.entity.ReviewRanking;
import com.codeit.sb01_deokhugam.domain.review.repository.PopularReviewRepository;
import com.codeit.sb01_deokhugam.domain.review.repository.ReviewLikeRepository;
import com.codeit.sb01_deokhugam.domain.review.repository.ReviewRepository;
import com.codeit.sb01_deokhugam.domain.user.repository.UserRepository;
import com.codeit.sb01_deokhugam.domain.user.service.PowerUserService;
import com.codeit.sb01_deokhugam.global.enumType.Period;
import com.codeit.sb01_deokhugam.global.schedule.utils.ScheduleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularReviewBatchService {

	private final CommentRepository commentRepository;
	private final ReviewLikeRepository likeRepository;
	private final ReviewRepository reviewRepository;
	private final PopularReviewRepository popularReviewRepository;
	private final UserRepository userRepository;
	private final NotificationRepository notificationRepository;
	private final PowerUserService powerUserService;

	/**
	 * 매일 자정(00:00)에 모든 기간(DAILY, WEEKLY, MONTHLY, ALL_TIME)에 대해
	 * 인기 리뷰 점수 계산 & 저장을 순차적으로 실행합니다.
	 */
	@Transactional
	@Scheduled(cron = "0 0 0 * * *")
	public void runBatch() {
		log.info("===== 인기 리뷰 배치 시작: {} =====", Instant.now());
		for (Period period : Period.values()) {
			log.info("Period={} 인기 리뷰 계산 시작", period);
			calculatePopularReviews(period);
			log.info("Period={} 인기 리뷰 계산 완료", period);
		}
		log.info("===== 인기 리뷰 배치 종료 =====");

		log.info("===== 파워유저 배치 시작 =====");
		powerUserService.calculateAllPeriodRankings();
		log.info("===== 파워유저 배치 종료 =====");
	}

	/**
	 * 주어진 기간(period)에 해당하는 댓글·좋아요를 집계하여
	 * 인기 리뷰 순위를 계산하고 저장합니다.
	 */
	@Transactional
	public void calculatePopularReviews(Period period) {
		// 1) 시작·종료 시각 계산
		Map.Entry<Instant, Instant> range = ScheduleUtils.getStartAndEndByPeriod(period);
		Instant start = range.getKey();
		Instant end = range.getValue();

		// 2) 기간 내 댓글·좋아요 모두 조회
		List<Comment> comments = commentRepository.findByCreatedAtBetweenAndDeletedFalse(start, end);
		List<ReviewLike> likes = likeRepository.findByCreatedAtBetween(start, end);

		// 3) 댓글·좋아요가 달린 모든 리뷰 ID 집합
		Set<UUID> candidateReviewIds = Stream
			.concat(comments.stream().map(Comment::getReviewId),
				likes.stream().map(ReviewLike::getReviewId))
			.collect(Collectors.toSet());
		if (candidateReviewIds.isEmpty()) {
			log.info("기간 {}: 집계 대상 리뷰 없음", period);
			return;
		}

		// 4) 실제로 존재하고 논리삭제 되지 않은 리뷰만 필터링
		List<Review> validReviews = reviewRepository.findAllById(candidateReviewIds);
		Set<UUID> validReviewIds = validReviews.stream()
			.filter(r -> !r.isDeleted())
			.map(Review::getId)
			.collect(Collectors.toSet());
		if (validReviewIds.isEmpty()) {
			log.info("기간 {}: 유효 리뷰 없음", period);
			return;
		}

		// 5) 리뷰별 좋아요·댓글 수 집계용 클래스
		record Counts(int[] likes, int[] comments) {
			Counts() {
				this(new int[] {0}, new int[] {0});
			}

			void incLike() {
				likes[0]++;
			}

			void incComment() {
				comments[0]++;
			}
		}
		Map<UUID, Counts> counts = new HashMap<>();

		// 6) 댓글 집계
		comments.stream()
			.map(Comment::getReviewId)
			.filter(validReviewIds::contains)
			.forEach(rid -> counts
				.computeIfAbsent(rid, id -> new Counts())
				.incComment()
			);

		// 7) 좋아요 집계
		likes.stream()
			.map(ReviewLike::getReviewId)
			.filter(validReviewIds::contains)
			.forEach(rid -> counts
				.computeIfAbsent(rid, id -> new Counts())
				.incLike()
			);

		// 8) 점수 계산·정렬·저장
		List<ReviewRanking> populars = new ArrayList<>();
		int rank = 1;
		for (Map.Entry<UUID, Counts> entry : counts.entrySet().stream()
			.sorted(Comparator.comparingDouble(e ->
				-(e.getValue().likes[0] * 0.3 + e.getValue().comments[0] * 0.7)
			))
			.toList()) {

			UUID reviewId = entry.getKey();
			Counts ct = entry.getValue();
			Review rv = validReviews.stream()
				.filter(r -> r.getId().equals(reviewId))
				.findFirst()
				.orElseThrow();

			BigDecimal score = BigDecimal.valueOf(ct.likes[0])
				.multiply(BigDecimal.valueOf(0.3))
				.add(BigDecimal.valueOf(ct.comments[0]).multiply(BigDecimal.valueOf(0.7)))
				.setScale(1, RoundingMode.HALF_UP);

			ReviewRanking ranking = ReviewRanking.builder()
				.review(rv)
				.bookId(rv.getBook().getId())
				.bookTitle(rv.getBook().getTitle())
				.bookThumbnailUrl(rv.getBook().getThumbnailUrl())
				.userId(rv.getAuthor().getId())
				.userNickname(rv.getAuthor().getNickname())
				.reviewRating(rv.getRating())
				.period(period)
				.likeCount(ct.likes[0])
				.commentCount(ct.comments[0])
				.score(score)
				.rank(rank++)
				.build();

			sendPopularReviewNotification(period, rv, rank);
			populars.add(ranking);
		}

		// 9) 기존 자료 삭제 후 저장
		popularReviewRepository.deleteByPeriod(period);
		popularReviewRepository.saveAll(populars);
		log.info("기간 {}: 인기 리뷰 {}건 저장 완료", period, populars.size());
	}

	private void sendPopularReviewNotification(Period period, Review rv, int ranking) {
		Notification createdReviewRankingNotification =
			Notification.ofPopularReview(rv.getAuthor(), rv, period, ranking);
		notificationRepository.save(createdReviewRankingNotification);
	}

}
