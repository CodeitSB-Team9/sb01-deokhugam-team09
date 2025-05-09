package com.codeit.sb01_deokhugam.domain.user.controller;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import com.codeit.sb01_deokhugam.domain.review.service.PopularReviewBatchService;
import com.codeit.sb01_deokhugam.domain.user.entity.PowerUser;
import com.codeit.sb01_deokhugam.domain.user.repository.PowerUserRankingRepository;
import com.codeit.sb01_deokhugam.global.enumType.Period;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PowerUserControllerTest {

	@LocalServerPort
	int port;

	@Autowired
	private PowerUserRankingRepository powerUserRankingRepository;

	@Mock // To avoid running actual batch logic
	private PopularReviewBatchService popularReviewBatchService;

	@BeforeEach
	void setUp() {
		RestAssured.port = port;
		powerUserRankingRepository.deleteAll();
	}

	@DisplayName("파워유저 목록 조회 성공 테스트 - 다음 페이지 없으므로 커서 미반환")
	@Test
	void givenValidRequest_whenFindPowerUsers_thenReturnPageDtoWith200AndNoCursor() {
		// given
		PowerUser user1 = new PowerUser(UUID.randomUUID(), "파워유저1", Period.DAILY, 1, new BigDecimal("99.9"),
			new BigDecimal("80.0"), 10, 5);
		PowerUser user2 = new PowerUser(UUID.randomUUID(), "파워유저2", Period.DAILY, 2, new BigDecimal("88.8"),
			new BigDecimal("70.0"), 8, 3);
		powerUserRankingRepository.saveAll(List.of(user1, user2));

		// when & then
		ExtractableResponse<Response> response = given().log().all()
			.contentType(ContentType.JSON)
			.queryParam("period", "DAILY")
			.queryParam("direction", Sort.Direction.ASC)
			.queryParam("cursor", 0)
			.queryParam("limit", 10)
			.when().get("/api/users/power")
			.then().log().all()
			.statusCode(HttpStatus.OK.value())
			.extract();

		JsonPath result = response.jsonPath();
		List<?> content = result.getList("content");
		assertThat(content).hasSize(2);
		assertThat(result.getInt("size")).isEqualTo(2);
		assertThat(result.getBoolean("hasNext")).isFalse();
		assertThat(result.getString("nextCursor")).isEqualTo("0");
		assertThat(result.getString("nextAfter")).isNull();
		// Check fields of first user
		assertThat(result.getString("content[0].nickname")).isEqualTo("파워유저1");
		assertThat(result.getString("content[0].period")).isEqualTo("DAILY");
		assertThat(result.getInt("content[0].rank")).isEqualTo(1);
		assertThat(result.getDouble("content[0].score")).isEqualTo(99.9);
		assertThat(result.getInt("content[0].likeCount")).isEqualTo(10);
		assertThat(result.getInt("content[0].commentCount")).isEqualTo(5);
	}

	@DisplayName("파워유저 목록 조회 성공 테스트 - 다음 페이지 존재하므로 커서를 함께 반환")
	@Test
	void givenValidRequest_whenFindPowerUsers_thenReturnPageDtoWith200AndCursor() {
		// given
		//파워유저 열명 만들어서 저장
		List<PowerUser> powerUsers = IntStream.range(1, 10)
			.mapToObj(i -> new PowerUser(
				UUID.randomUUID(),
				"파워유저" + i,
				Period.DAILY,
				i,
				new BigDecimal("99.0"),
				BigDecimal.valueOf(80.0),
				10 - i,
				10 - i
			))
			.toList();
		powerUserRankingRepository.saveAll(powerUsers);
		// when & then
		ExtractableResponse<Response> response = given().log().all()
			.contentType(ContentType.JSON)
			.queryParam("period", "DAILY")
			.queryParam("direction", Sort.Direction.ASC)
			.queryParam("cursor", 0)
			.queryParam("limit", 5)
			.when().get("/api/users/power")
			.then().log().all()
			.statusCode(HttpStatus.OK.value())
			.extract();

		JsonPath result = response.jsonPath();
		List<?> content = result.getList("content");
		assertThat(content).hasSize(5);
		assertThat(result.getInt("size")).isEqualTo(5);
		assertThat(result.getBoolean("hasNext")).isTrue();
		assertThat(result.getString("nextCursor")).isEqualTo("5");
		assertThat(
			result.getString("nextAfter")).isNotNull(); //todo 파워유저 생성방식때문에 createdAt이 테스터와 동일하게 생성되지 않음. 작동방식 변경후 수정필요

		assertThat(result.getString("content[0].nickname")).isEqualTo("파워유저1");
		assertThat(result.getString("content[0].period")).isEqualTo("DAILY");
		assertThat(result.getInt("content[0].rank")).isEqualTo(1);
		assertThat(result.getDouble("content[0].score")).isEqualTo(99.0);
		assertThat(result.getInt("content[0].likeCount")).isEqualTo(9);
		assertThat(result.getInt("content[0].commentCount")).isEqualTo(9);
	}

	@DisplayName("파워유저 목록 조회 실패 테스트 - 존재하지 않는 period 파라미터")
	@Test
	void givenInvalidPeriod_whenFindPowerUsers_thenReturn400() {
		ExtractableResponse<Response> response = given().log().all()
			.queryParam("period", "INVALID")
			.when().get("/api/users/power")
			.then().log().all()
			.statusCode(HttpStatus.BAD_REQUEST.value())
			.extract();

		JsonPath result = response.jsonPath();
		assertThat(result.getString("code")).contains("MethodArgumentTypeMismatchException");
		assertThat(result.getString("message")).contains("Failed to convert");
	}

	@DisplayName("파워유저 목록 조회 실패 테스트 - 존재하지 않는 direction 파라미터")
	@Test
	void givenInvalidDirection_whenFindPowerUsers_thenReturn400() {
		ExtractableResponse<Response> response = given().log().all()
			.queryParam("direction", "INVALID")
			.when().get("/api/users/power")
			.then().log().all()
			.statusCode(HttpStatus.BAD_REQUEST.value())
			.extract();

		JsonPath result = response.jsonPath();
		assertThat(result.getString("code")).contains("MethodArgumentTypeMismatchException");
		assertThat(result.getString("message")).contains("Failed to convert");
	}

	@DisplayName("파워유저 목록 조회 실패 테스트 - 잘못된 cursor 파라미터(문자열)")
	@Test
	void givenInvalidCursor_whenFindPowerUsers_thenReturn400() {
		ExtractableResponse<Response> response = given().log().all()
			.queryParam("cursor", "abc")
			.when().get("/api/users/power")
			.then().log().all()
			.statusCode(HttpStatus.BAD_REQUEST.value())
			.extract();

		JsonPath result = response.jsonPath();
		assertThat(result.getString("code")).contains("MethodArgumentTypeMismatchException");
		assertThat(result.getString("message")).contains("Failed to convert");
	}

	@DisplayName("파워유저 목록 조회 실패 테스트 - 잘못된 after 파라미터(날짜 형식 아님)")
	@Test
	void givenInvalidAfter_whenFindPowerUsers_thenReturn400() {
		ExtractableResponse<Response> response = given().log().all()
			.queryParam("after", "not-a-date")
			.when().get("/api/users/power")
			.then().log().all()
			.statusCode(HttpStatus.BAD_REQUEST.value())
			.extract();

		JsonPath result = response.jsonPath();
		assertThat(result.getString("code")).contains("MethodArgumentTypeMismatchException");
		assertThat(result.getString("message")).contains("Failed to convert");
	}
}
