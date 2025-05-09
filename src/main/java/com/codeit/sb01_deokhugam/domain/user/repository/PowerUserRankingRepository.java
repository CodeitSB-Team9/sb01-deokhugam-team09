package com.codeit.sb01_deokhugam.domain.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.codeit.sb01_deokhugam.domain.user.entity.PowerUser;

//todo 논리삭제된 유저는 연산에서 빼기
public interface PowerUserRankingRepository extends JpaRepository<PowerUser, UUID> {
	@Query(value = """
			SELECT
				gen_random_uuid() as id,
				CAST(:period AS VARCHAR) as period,
				ROW_NUMBER() OVER (
					ORDER BY score DESC, user_created_at ASC
				) AS rank,
				CAST(score AS NUMERIC(10,2)) AS score,
				CAST(review_score_sum AS NUMERIC(10,2)) AS review_score_sum,
				COALESCE(like_count, 0) AS like_count,
				COALESCE(comment_count, 0) AS comment_count,
				user_id,
				nickname,
				CURRENT_TIMESTAMP as created_at,
				CURRENT_TIMESTAMP as updated_at
			FROM (
				SELECT
					CAST(:period AS VARCHAR) AS period,
					CAST(
					COALESCE(SUM(r.like_count), 0) * 0.5
				   	+ COUNT(DISTINCT l.id) * 0.2
				   	+ COUNT(DISTINCT c.id) * 0.3
				 	AS NUMERIC(10,2)) AS score,
					COALESCE(SUM(rr.score), 0) AS review_score_sum,
					COALESCE(SUM(r.like_count), 0) AS like_count,
					COUNT(DISTINCT c.id) AS comment_count,
					u.id AS user_id,
					u.nickname,
					u.created_at as user_created_at
				FROM users u
				LEFT JOIN reviews r
					ON r.user_id = u.id AND r.created_at BETWEEN :start AND :end
				LEFT JOIN review_rankings rr
					ON rr.user_id = u.id AND rr.period = CAST(:period AS VARCHAR)
				LEFT JOIN comments c
					ON c.user_id = u.id AND c.created_at BETWEEN :start AND :end
				LEFT JOIN review_likes l
					ON l.user_id = u.id AND l.created_at BETWEEN :start AND :end
				WHERE u.is_deleted = false
				GROUP BY u.id, u.nickname, u.created_at
			) AS ranked_users
			ORDER BY rank;
		""", nativeQuery = true)
	List<PowerUser> calculatePowerUserRank(
		@Param("start") Instant start,
		@Param("end") Instant end,
		@Param("period") String period
	);
}
