package com.codeit.sb01_deokhugam.domain.book.exception;

import com.codeit.sb01_deokhugam.global.exception.DeokhugamException;
import com.codeit.sb01_deokhugam.global.exception.ErrorCode;

public class BookException extends DeokhugamException {
	public BookException(ErrorCode errorCode) {
		super(errorCode);
	}
}
