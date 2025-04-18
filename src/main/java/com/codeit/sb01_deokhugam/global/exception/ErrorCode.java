package com.codeit.sb01_deokhugam.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	//global
	INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."), INVALID_REQUEST("잘못된 요청입니다"), ILLEGAL_ARGUMENT_ERROR(
		"잘못된 인수가 전달되었습니다."),

	//book
	BOOK_NOT_FOUND("해당 도서가 존재하지 않습니다."),

	//comment
	COMMENT_NOT_FOUND("해당 댓글이 존재하지 않습니다."),

	//notification
	NOTIFICATION_NOT_FOUND("해당 알림이 존재하지 않습니다."),

	//review
	REVIEW_NOT_FOUND("해당 리뷰가 존재하지 않습니다"),

	//user
	USER_NOT_FOUND("해당 유저가 존재하지 않습니다."),
	;

	private final String message; //메시지

}
