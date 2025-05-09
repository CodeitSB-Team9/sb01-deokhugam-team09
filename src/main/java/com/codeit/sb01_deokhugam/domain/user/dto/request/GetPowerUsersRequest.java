package com.codeit.sb01_deokhugam.domain.user.dto.request;

import java.time.Instant;

import org.springframework.data.domain.Sort;

import com.codeit.sb01_deokhugam.global.enumType.Period;

public record GetPowerUsersRequest(
	Period period,
	Sort.Direction direction,
	int cursor,
	Instant after,
	int limit
) {
}
