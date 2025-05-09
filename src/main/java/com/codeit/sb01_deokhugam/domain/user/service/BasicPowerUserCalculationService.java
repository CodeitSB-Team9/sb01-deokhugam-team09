package com.codeit.sb01_deokhugam.domain.user.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codeit.sb01_deokhugam.domain.user.entity.PowerUser;
import com.codeit.sb01_deokhugam.domain.user.repository.PowerUserRankingRepository;
import com.codeit.sb01_deokhugam.global.enumType.Period;
import com.codeit.sb01_deokhugam.global.schedule.utils.ScheduleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicPowerUserCalculationService implements PowerUserCalculationService {

	private final PowerUserRankingRepository powerUserRankingRepository;

	@Transactional
	@Override
	public long calculateRankingsForPeriod(Period period) {
		//log.info("유저 랭킹 계산 기간: {}", period);

		Map.Entry<Instant, Instant> startAndEndTime = ScheduleUtils.getStartAndEndByPeriod(period);
		//log.info("계산 기간: {} ~ {}", startAndEndTime.getKey(), startAndEndTime.getValue());

		List<PowerUser> powerUsers;
		try {
			powerUsers = powerUserRankingRepository.calculatePowerUserRank(
				startAndEndTime.getKey(), startAndEndTime.getValue(), period.toString());

			//todo 차후 구조 개선 필요( 엔티티로 파싱되었지만 id 오류 발생하는 값 받아서 엔티티 재생성
			List<PowerUser> target = powerUsers.stream()
				.map(powerUser -> {
					return new PowerUser(
						powerUser.getUserId(),
						powerUser.getNickname(),
						powerUser.getPeriod(),
						powerUser.getRank(),
						powerUser.getScore(),
						powerUser.getReviewScoreSum(),
						powerUser.getLikeCount(),
						powerUser.getCommentCount()
					);
				}).toList();

			try {
				powerUserRankingRepository.saveAll(target);
				//log.info("saveAll 호출 완료");
			} catch (Exception e) {
				//log.error("유저랭킹 생성 후 saveAll 중 예외 발생: {}", e.getMessage(), e);
				throw e;
			}
			// 저장 후 데이터 확인
			List<PowerUser> savedUsers = powerUserRankingRepository.findAll();
			log.info("저장된 총 파워유저 수: {}", savedUsers.size());

		} catch (Exception e) {
			//log.error("파워유저 랭킹 계산 중 오류 발생: {}", e.getMessage(), e);
			throw new RuntimeException("파워유저 랭킹 계산 실패", e);
		}

		return (long)powerUsers.size();
	}
}
