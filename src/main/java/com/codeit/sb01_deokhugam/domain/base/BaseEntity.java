package com.codeit.sb01_deokhugam.domain.base;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class) //생성자, 수정자, 생성/수정 날짜 자동 관리
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid", updatable = false, nullable = false)
	private UUID id;

	@CreatedDate
	@Column(columnDefinition = "timestamp with time zone", updatable = false, nullable = false)
	private Instant createdAt;

}
