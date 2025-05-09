package com.codeit.sb01_deokhugam.domain.user.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.sb01_deokhugam.domain.user.dto.request.GetPowerUsersRequest;
import com.codeit.sb01_deokhugam.domain.user.dto.response.PowerUserDto;
import com.codeit.sb01_deokhugam.domain.user.entity.PowerUser;
import com.codeit.sb01_deokhugam.domain.user.mapper.PowerUserMapper;
import com.codeit.sb01_deokhugam.domain.user.repository.PowerUserRankingRepository;
import com.codeit.sb01_deokhugam.domain.user.repository.PowerUserSearchRepository;
import com.codeit.sb01_deokhugam.global.dto.response.PageResponse;
import com.codeit.sb01_deokhugam.global.enumType.Period;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class BasicPowerUserService implements PowerUserService {

	private final PowerUserSearchRepository powerUserSearchRepository;
	private final PowerUserRankingRepository powerUserRankingRepository;
	private final PowerUserCalculationService powerUserCalculationService;
	private final PowerUserMapper powerUserMapper;

	private Map<Period, Long> userNumberForPeriod = new HashMap<>();

	@Override
	public PageResponse<PowerUserDto> findPowerUsers(GetPowerUsersRequest getPowerUsersRequest) {
		int limitSize = getPowerUsersRequest.limit();
		Period period = getPowerUsersRequest.period();

		List<PowerUser> powerUsers = powerUserSearchRepository.findPowerUsers(getPowerUsersRequest);

		//size+1 결과에 따라 다음페이지 존재여부 설정 후, 추가로 받아온 요소 하나 삭제
		boolean hasNext = (powerUsers.size() > limitSize);
		powerUsers = hasNext ? powerUsers.subList(0, limitSize) : powerUsers;
		int size = powerUsers.size();

		PowerUser lastUser = (powerUsers.isEmpty() ? null : powerUsers.get(powerUsers.size() - 1));
		int nextCursor = (lastUser != null) && hasNext ? lastUser.getRank() : 0;
		Instant nextAfter = (lastUser != null) && hasNext ? lastUser.getCreatedAt() : null;
		Long totalElements = this.userNumberForPeriod.get(period);

		List<PowerUserDto> powerUserDtoList = powerUserMapper.toDtoList(powerUsers);

		return new PageResponse<>(powerUserDtoList, nextAfter, nextCursor,
			size, hasNext, totalElements);
	}

	@Transactional
	@Override
	public void calculateAllPeriodRankings() {
		//log.info("유저 랭킹 계산 시작");

		// 매일 기존 랭킹 데이터 삭제 후 기간별로 새로 계산한 데이터들을 저장한다.
		powerUserRankingRepository.deleteAll();

		userNumberForPeriod.put(Period.DAILY, powerUserCalculationService.calculateRankingsForPeriod(Period.DAILY));
		userNumberForPeriod.put(Period.WEEKLY, powerUserCalculationService.calculateRankingsForPeriod(Period.WEEKLY));
		userNumberForPeriod.put(Period.MONTHLY, powerUserCalculationService.calculateRankingsForPeriod(Period.MONTHLY));
		userNumberForPeriod.put(Period.ALL_TIME,
			powerUserCalculationService.calculateRankingsForPeriod(Period.ALL_TIME));

		//log.info("유저 랭킹 계산 성공");
	}
}
