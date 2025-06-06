package com.codeit.sb01_deokhugam.domain.book.dto;

import java.time.LocalDate;

public record NaverBookDto(
	String title,
	String author,
	String description,
	String publisher,
	LocalDate publishedDate,
	String isbn,
	String thumbnailImage //byte[] -> String변경
) {
}
