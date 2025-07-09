package com.codeit.sb01_deokhugam.domain.book.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.codeit.sb01_deokhugam.domain.book.entity.Book;

public interface BookRepository extends JpaRepository<Book, UUID>, BookRepositoryCustom {
	//JpaRepository 쿼리메소드
	boolean existsByIsbn(String isbn);

	@Modifying
	@Query("""
		UPDATE Book b
		SET b.reviewCount = (
				SELECT COUNT(r)
				FROM Review r
				WHERE r.book.id   = :bookId
				AND r.deleted   = false
		),
		b.rating = (
			SELECT COALESCE(AVG(r.rating), 0)
			FROM Review r
			WHERE r.book.id   = :bookId
			AND r.deleted   = false
		)
		WHERE b.id = :bookId
		""")
	void recalcStats(@Param("bookId") UUID bookId);

	Book findByIsbn(String isbn);

	boolean existsByIsbnAndDeletedFalse(String isbn);

	boolean existsByIsbnAndDeletedTrue(String isbn);
}
