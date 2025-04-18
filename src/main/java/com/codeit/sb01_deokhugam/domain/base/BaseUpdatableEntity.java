package com.codeit.sb01_deokhugam.domain.base;

import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@Getter
public abstract class BaseUpdatableEntity extends BaseEntity {

	@LastModifiedDate
	@Column(columnDefinition = "timestamp with time zone")
	private Instant updatedAt;

}

