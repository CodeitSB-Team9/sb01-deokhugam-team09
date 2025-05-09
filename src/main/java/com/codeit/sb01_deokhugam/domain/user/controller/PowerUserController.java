package com.codeit.sb01_deokhugam.domain.user.controller;

import java.time.Instant;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codeit.sb01_deokhugam.domain.review.service.PopularReviewBatchService;
import com.codeit.sb01_deokhugam.domain.user.dto.request.GetPowerUsersRequest;
import com.codeit.sb01_deokhugam.domain.user.dto.response.PowerUserDto;
import com.codeit.sb01_deokhugam.domain.user.service.PowerUserService;
import com.codeit.sb01_deokhugam.global.dto.response.PageResponse;
import com.codeit.sb01_deokhugam.global.enumType.Period;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/users/power")
@RequiredArgsConstructor
public class PowerUserController {

	private final PowerUserService powerUserService;
	private final PopularReviewBatchService reviewService;//todo 지우기

	// 파워유저 목록조회
	@GetMapping
	public ResponseEntity<PageResponse<PowerUserDto>> findPowerUsers(
		// 값이 들어왔는데 변환할 타입과 맞지않을경우(ex. period에 지정한 상수 외 다른 문자 들어옴) 스프링이 400 반환)
		@RequestParam(value = "period", defaultValue = "DAILY") Period period,
		@RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
		@RequestParam(value = "cursor", defaultValue = "0") int cursor, //조회된 페이지 마지막 등수
		@RequestParam(value = "after", required = false) Instant after, //조회된 페이지 마지막 생성일
		@RequestParam(value = "limit", defaultValue = "50") @Min(value = 0, message = "limit은 음수일 수 없습니다.") int limit) {
		GetPowerUsersRequest getPowerUsersRequest = new GetPowerUsersRequest(period, direction, cursor, after, limit);
		PageResponse<PowerUserDto> powerUsers = powerUserService.findPowerUsers(getPowerUsersRequest);
		return ResponseEntity.status(HttpStatus.OK).body(powerUsers);
	}
}
