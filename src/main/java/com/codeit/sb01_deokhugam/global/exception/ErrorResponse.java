package com.codeit.sb01_deokhugam.global.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {

	private final Instant timestamp;
	private final String code;
	private final String message;
	private final Map<String, Object> details;
	private final String exceptionType;
	private final int status;

	public ErrorResponse(DeokhugamException exception, int status) {
		this(Instant.now(), exception.getErrorCode().name(), exception.getMessage(), exception.getDetails(),
			exception.getClass().getSimpleName(), status);
	}

	public ErrorResponse(Exception exception, int status) {
		this(Instant.now(), exception.getClass().getSimpleName(), exception.getMessage(), new HashMap<>(),
			exception.getClass().getSimpleName(), status);
	}

	public ErrorResponse(List<FieldError> fieldErrors, int status) {
		this(
			Instant.now(),
			"VALIDATION_ERROR",
			"요청 데이터가 유효하지 않습니다.",
			convertFieldErrorsToDetails(fieldErrors),
			MethodArgumentNotValidException.class.getSimpleName(),
			status
		);
	}

	private static Map<String, Object> convertFieldErrorsToDetails(List<FieldError> fieldErrors) {
		Map<String, Object> details = new HashMap<>();
		for (FieldError error : fieldErrors) {
			details.put(error.getField(), error.getDefaultMessage());
		}
		return details;
	}

}
