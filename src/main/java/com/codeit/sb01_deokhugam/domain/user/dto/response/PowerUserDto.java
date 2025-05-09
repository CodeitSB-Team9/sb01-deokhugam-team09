package com.codeit.sb01_deokhugam.domain.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.codeit.sb01_deokhugam.global.enumType.Period;

public record PowerUserDto(
	UUID userId,
	String nickname,
	Period period, //랭킹 조회시의 기간조건
	Instant createdAt,
	int rank,
	double score,
	double reviewScoreSum,
	int likeCount,
	int commentCount
) {
}
