package com.codeit.sb01_deokhugam.domain.book.entity;

import com.codeit.sb01_deokhugam.domain.base.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "book")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseEntity {

}
