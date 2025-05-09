package com.codeit.sb01_deokhugam.domain.user.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.codeit.sb01_deokhugam.domain.base.BaseEntity;
import com.codeit.sb01_deokhugam.global.enumType.Period;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_rankings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PowerUser extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "period", nullable = false)
	Period period;
	@Column(name = "rank", nullable = false)
	int rank;
	@Column(name = "score", precision = 2, scale = 1)
	BigDecimal score;
	@Column(name = "review_score_sum")
	BigDecimal reviewScoreSum;
	@Column(name = "like_count", nullable = false)
	int likeCount;
	@Column(name = "comment_count", nullable = false)
	int commentCount;
	@Column(name = "user_id", nullable = false)
	private UUID userId;
	@Column(name = "nickname", nullable = false, length = 20)
	private String nickname;

	//todo 소수점 1자리까지만 나타낼건데 테이블에 반올림한 데이터로 저장할지(정확성 내려감. 컬럼에 프리시전, 스케일 속성 추가하기) 데이터 제공시에 반올림처리해서 내보낼지 고민하기
	public PowerUser(UUID userId, String nickname, Period period, int rank, BigDecimal score, BigDecimal reviewScoreSum,
		int likeCount, int commentCount) {
		if (userId == null || nickname == null || period == null || rank < 0 || score.doubleValue() < 0.0
			|| reviewScoreSum.doubleValue() < 0.0 || likeCount < 0 || commentCount < 0) {
			throw new IllegalArgumentException("유효한 값을 입력해주세요.");
		}
		if (nickname.length() < 2 || nickname.length() > 20) {
			throw new IllegalArgumentException("닉네임은 2자 이상 20자 이하로 입력해주세요.");
		}

		this.userId = userId;
		this.nickname = nickname;
		this.period = period;
		this.rank = rank;
		this.score = score;
		this.reviewScoreSum = reviewScoreSum;
		this.likeCount = likeCount;
		this.commentCount = commentCount;
	}
}
