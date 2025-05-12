package com.codeit.sb01_deokhugam.domain.book.service;

import static com.codeit.sb01_deokhugam.domain.book.entity.QBook.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import com.codeit.sb01_deokhugam.domain.book.dto.BookCreateRequest;
import com.codeit.sb01_deokhugam.domain.book.dto.BookDto;
import com.codeit.sb01_deokhugam.domain.book.dto.BookRankingCalculation;
import com.codeit.sb01_deokhugam.domain.book.dto.BookUpdateRequest;
import com.codeit.sb01_deokhugam.domain.book.dto.PopularBookDto;
import com.codeit.sb01_deokhugam.domain.book.entity.Book;
import com.codeit.sb01_deokhugam.domain.book.entity.BookRanking;
import com.codeit.sb01_deokhugam.domain.book.exception.BookNotFoundException;
import com.codeit.sb01_deokhugam.domain.book.exception.IsbnAlreadyExistsException;
import com.codeit.sb01_deokhugam.domain.book.mapper.BookMapper;
import com.codeit.sb01_deokhugam.domain.book.mapper.PopularBookMapper;
import com.codeit.sb01_deokhugam.domain.book.repository.BookRepository;
import com.codeit.sb01_deokhugam.domain.book.repository.PopularBookRepository;
import com.codeit.sb01_deokhugam.domain.review.entity.Review;
import com.codeit.sb01_deokhugam.domain.review.repository.ReviewRepository;
import com.codeit.sb01_deokhugam.global.dto.response.PageResponse;
import com.codeit.sb01_deokhugam.global.enumType.Period;
import com.codeit.sb01_deokhugam.global.s3.S3Service;
import com.codeit.sb01_deokhugam.global.schedule.utils.ScheduleUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

	private final BookRepository bookRepository;
	private final BookMapper bookMapper;
	//image OCR
	private final Tesseract tesseract;
	private final PopularBookRepository popularBookRepository;
	private final PopularBookMapper popularBookMapper;
	private final ReviewRepository reviewRepository;

	private final S3Service s3Service;

	//S3이미지 저장 디렉토리
	private final String directory = book.getClass().getSimpleName();

	/**
	 * 도서정보를 DB에 저장합니다.
	 *
	 * @param bookCreateRequest
	 * @param thumnailImage
	 * @return 저장한 도서의 DTO
	 * @throws IOException
	 */
	@Transactional
	public BookDto create(BookCreateRequest bookCreateRequest, MultipartFile thumnailImage) {

		// isbn 중복 검증
		if (bookRepository.existsByIsbnAndDeletedFalse(bookCreateRequest.isbn())) {
			throw new IsbnAlreadyExistsException().withIsbn(bookCreateRequest.isbn());
		}
		// 논리삭제된 책을 재등록하는 과정
		else if (bookRepository.existsByIsbnAndDeletedTrue(bookCreateRequest.isbn())) {
			Book deletedBook = bookRepository.findByIsbn(bookCreateRequest.isbn());
			deletedBook.undoSoftDelete();  // is_deleted를 false로 변경
			// 도서 정보를 업데이트
			deletedBook.update(
				bookCreateRequest.title(),
				bookCreateRequest.author(),
				bookCreateRequest.description(),
				bookCreateRequest.publisher(),
				bookCreateRequest.publishedDate(),
				s3Service.upload(thumnailImage, directory)
			);
			return bookMapper.toDto(deletedBook);
		}

		/**새로운 isbn을 등록하는 과정**/

		//S3에 이미지를 저장하고, url을 가져온다.
		String imageUrl = s3Service.upload(thumnailImage, directory);

		//책 entity를 생성한다.
		Book createdBook = new Book(
			bookCreateRequest.title(),
			bookCreateRequest.author(), bookCreateRequest.description(), bookCreateRequest.isbn(),
			bookCreateRequest.publisher(), bookCreateRequest.publishedDate(), imageUrl, 0, new BigDecimal("0.0"), false
		);

		//DB에 도서 엔티티 저장
		bookRepository.save(createdBook);

		//도서 Dto 반환
		return bookMapper.toDto(createdBook);
	}

	/** 도서 목록을 정렬기준과 필터링으로 커서 기반 페이지네이션으로 조회합니다.
	 * @param keyword
	 * @param after
	 * @param cursor
	 * @param orderBy
	 * @param direction
	 * @param limit
	 * @return BookDto 페이지리스폰스
	 */
	@Transactional
	//도서 목록 조회
	public PageResponse<BookDto> findAllWithCursor(String keyword, Instant after, String cursor, String orderBy,
		String direction, int limit) {

		//정렬기준 예외처리
		Set<String> validOrderBys = Set.of("title", "publishedDate", "rating", "reviewCount");
		if (!validOrderBys.contains(orderBy)) {
			throw new IllegalArgumentException("유효하지 않은 정렬 기준입니다: " + orderBy);
		}

		List<Book> books = bookRepository.findListByCursor(keyword, after, cursor, orderBy, direction, limit + 1);

		// 실제 size 계산 (초과 조회된 1개는 제외)
		int fetchedSize = books.size();
		boolean hasNext = fetchedSize > limit;

		// 실제로 보여줄 limit 개수만큼만 남기기
		List<Book> resultBooks = hasNext ? books.subList(0, limit) : books;
		// DTO변환
		List<BookDto> bookDtos = resultBooks.stream()
			.map(bookMapper::toDto)
			.toList();
		int size = resultBooks.size();

		//TODO: 매번 호출 비효율적.
		//totalElements 계산
		Long totalElements = bookRepository.getTotalElements(keyword);

		//nextCursor 조회. hasNext가 있으면 존재.
		String nextCursor = hasNext ? convertCursor(orderBy, books.get(size - 1)) : null;

		//nextAfter 조회
		Instant nextAfter = hasNext ? bookDtos.get(size - 1).createdAt() : null;

		return new PageResponse<>(bookDtos, nextAfter, nextCursor, size, hasNext, totalElements);
	}

	//넥스트 커서 값 반환
	private String convertCursor(String sortBy, Book book) {
		switch (sortBy) {
			case "title":
				return book.getTitle();
			case "publishedDate":
				return book.getPublishedDate().toString();
			case "rating":
				return book.getRating().toString();
			case "reviewCount":
				return book.getReviewCount().toString();
			default:
				return null;
		}

	}

	/**
	 * 도서 id에 대한 도서 정보를 상세 조회합니다.
	 * @param bookId
	 * @return 도서 상세 정보
	 */
	@Transactional
	//도서 상세 정보 조회
	public BookDto findById(UUID bookId) {
		//log.debug("도서 조회 시작: id={}", bookId);
		BookDto bookDto = bookMapper.toDto(bookRepository.findByIdNotLogicalDelete(bookId).orElseThrow(
			() -> new BookNotFoundException().withId(bookId)
		));
		//log.info("도서 조회 완료: id={}", bookId);
		return bookDto;
	}

	/**
	 * 도서를 논리 삭제합니다.(soft delete)
	 * @param bookId
	 */
	@Transactional
	public void delete(UUID bookId) {
		//log.debug("도서 논리 삭제 시작: id={}", bookId);
		Book book = bookRepository.findByIdNotLogicalDelete(bookId).orElseThrow(
			() -> new BookNotFoundException().withId(bookId));
		book.softDelete(); //엔티티의 deleted를 true로 변경
		//log.info("도서 논리 삭제 완료: id={}", bookId);
	}

	/**
	 * 도서를 물리 삭제합니다. 관련된 리뷰와 댓글을 모두 삭제합니다.
	 *
	 * @param bookId
	 */
	@Transactional
	public void deletePhysical(UUID bookId) {
		//log.debug("도서 물리 삭제 시작: id={}", bookId);
		bookRepository.findById(bookId).orElseThrow(
			() -> new BookNotFoundException().withId(bookId));
		//log.debug("도서의 관련 리뷰 삭제 시작: id={}", bookId);
		bookRepository.deleteById(bookId);
		//TODO: 리뷰에서 물리삭제 확인 필요
		//reviewService.deleteByBookPhysicalDelete(bookId);
		//log.info("도서 물리 삭제 완료: id={}", bookId);
	}

	/**
	 * 도서 id에 대한 도서 정보를 수정합니다.
	 *
	 * @param bookId
	 * @param bookUpdateRequest
	 * @param thumnailImage
	 * @return 수정된 도서 DTO
	 * @throws IOException
	 */
	@Transactional
	public BookDto update(UUID bookId, BookUpdateRequest bookUpdateRequest, MultipartFile thumnailImage) {

		// 기존 도서 조회 (논리적 삭제 되지 않은 도서)
		Book book = bookRepository.findByIdNotLogicalDelete(bookId)
			.orElseThrow(() -> new BookNotFoundException().withId(bookId));

		// 이미지가 새로 들어온 경우에 S3에 업로드
		String imageUrl = book.getThumbnailUrl();
		if (thumnailImage != null) {
			imageUrl = s3Service.upload(thumnailImage, directory);
		}

		// 도서 정보 업데이트
		book.update(
			bookUpdateRequest.title(),
			bookUpdateRequest.author(),
			bookUpdateRequest.description(),
			bookUpdateRequest.publisher(),
			bookUpdateRequest.publishedDate(),
			imageUrl
		);

		return bookMapper.toDto(book);
	}

	/**
	 * 이미지에 있는 isbn을 읽어들여 isbn을 반환합니다.
	 * @param image
	 * @return isbn
	 * @throws IOException
	 * @throws TesseractException
	 */
	//TODO: 모든 예외를 도서 정보 등록 중 요류가.. 이거상속하게
	public String extractTextByOcr(MultipartFile image) throws IOException, TesseractException {

		if (image.isEmpty()) {
			throw new IllegalArgumentException("이미지가 등록되지 않았습니다");
		}

		// MultipartFile을 BufferedImage로 변환
		BufferedImage bufferedImage = ImageIO.read(image.getInputStream());

		// OCR 수행
		String result = tesseract.doOCR(bufferedImage);

		// 숫자만 추출하기 
		String isbn = result.replaceAll("[^0-9]", "");  // 숫자 외 다른 문자 제거

		// 이미지에 ISBN이 중복되어있는 케이스 처리
		if (isbn.startsWith("97") && isbn.length() >= 13 || isbn.startsWith("98") && isbn.length() >= 13) {
			// 국제표준 ISBN 접두부 97 혹은 98로 시작하는 13자리 ISBN 번호 반환
			return isbn.substring(0, 13);
		}

		return isbn;
	}

	/**
	 * 인기 도서 목록을 조회 기간에 대해 조회합니다.
	 * @param period
	 * @param after
	 * @param cursor
	 * @param direction
	 * @param limit
	 * @return 인기 도서 정보 페이지리스폰스
	 */
	public PageResponse<PopularBookDto> findPopularBook(String period, Instant after, String cursor, String direction,
		int limit) {

		// period가 enum에 속하는지 검증 ->
		try {
			Period.valueOf(period);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("잘못된 period가 전달되었습니다. period  : " + period);
		}

		List<BookRanking> bookRankings = popularBookRepository.findListByCursor(period, after, cursor, direction,
			limit + 1);

		// 실제 size 계산 (초과 조회된 1개는 제외)
		int fetchedSize = bookRankings.size();
		boolean hasNext = fetchedSize > limit;

		// 실제로 보여줄 limit 개수만큼만 남기기
		List<BookRanking> resultBooks = hasNext ? bookRankings.subList(0, limit) : bookRankings;

		List<PopularBookDto> popularBookDtos = resultBooks.stream()
			.map(popularBookMapper::toDto)
			.toList();
		int size = resultBooks.size();

		//TODO: 매번 호출 비효율적.
		//totalElements 계산
		Long totalElements = popularBookRepository.getTotalElements(period);

		//nextCursor 조회
		String nextCursor =
			hasNext ? String.valueOf(cursor == null ? limit : Integer.parseInt(cursor) + limit) : null;

		//nextAfter 조회
		Instant nextAfter = hasNext ? popularBookDtos.get(size - 1).createdAt() : null;

		return new PageResponse<>(popularBookDtos, nextAfter, nextCursor, size, hasNext, totalElements);

	}

	/**
	 * period에 대한 도서 랭킹 순위를 계산하고, 도서 랭킹 테이블에 저장합니다. 논리삭제 되지 않은 도서에 대해 연산합니다.
	 * @param period
	 */
	@Transactional
	public void calculateRanking(Period period) {

		//Period에 따라서, 조회할 리뷰의 시작과 끝 날짜를 계산한다.
		Map.Entry<Instant, Instant> range = ScheduleUtils.getStartAndEndByPeriod(period);
		Instant start = range.getKey();
		Instant end = range.getValue();

		// 리뷰 테이블에서, 날짜 범위에 해당하는 필요한 리뷰리스트를 가져온다
		List<Review> reviews = reviewRepository.findByCreatedAtBetween(start, end);

		// 리뷰에 포함된 도서 ID 목록을 추출한다.
		Set<UUID> bookIdsInReviews = reviews.stream()
			.map(review -> review.getBook().getId())
			.collect(Collectors.toSet());

		// 도서 정보 조회 (bookId 중 논리적 삭제되지 않은 도서만 필터링)
		// Function.identity()는 객체 자신을 그대로 반환함.
		Map<UUID, Book> bookMap = bookRepository.findAllById(bookIdsInReviews).stream()
			.filter(Book::logicalExists)
			.collect(Collectors.toMap(Book::getId, Function.identity()));
		// bookId 셋 생성
		Set<UUID> validBookIds = bookMap.keySet();

		// validBookIds을 이용하여 논리적 삭제되지 않은 도서에 해당하는 리뷰만 필터링
		List<Review> filteredReviews = reviews.stream()
			.filter(review -> validBookIds.contains(review.getBook().getId()))
			.collect(Collectors.toList());

		// 도서 ID 별로 리뷰를 그룹화하고 스코어를 계산한다.
		Map<UUID, BookRankingCalculation> bookCalculations = calculateBookRankingByReviews(filteredReviews);

		// Score 기준으로 내림차순 정렬한 도서 ID 리스트
		List<UUID> sortedBookIds = bookCalculations.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(
				Comparator.comparing(BookRankingCalculation::score).reversed()
			))
			.map(Map.Entry::getKey)
			.toList();

		// 정보에 대한 BookRanking을 생성한다.
		List<BookRanking> bookRankings = new ArrayList<>();
		int rank = 0;
		for (int i = 0; i < sortedBookIds.size(); i++) {
			//스코어 내림차순 순으로 book 엔티티를 가져온다. 
			UUID bookId = sortedBookIds.get(i);
			Book book = bookMap.get(bookId);

			BookRankingCalculation calc = bookCalculations.get(bookId);

			//동일 점수인 경우 동일 등수로 처리하는 로직
			if (i == 0 || !bookRankings.get(i - 1).getScore().equals(calc.score())) {
				rank = i + 1;
			}

			BookRanking ranking = new BookRanking(
				period,
				rank,
				calc.score(),
				calc.reviewCount(),
				calc.avgRating(),
				book.getThumbnailUrl(),
				book.getTitle(),
				book.getAuthor(),
				bookId
			);

			bookRankings.add(ranking);
		}

		// bookRanking테이블에 엔티티들을 저장한다.
		popularBookRepository.saveAll(bookRankings);
	}

	//리뷰 리스트에서 도서id에 대해 그룹화하고, 도서에 대한 리뷰수, 평점평균, 스코어를 계산한다.
	private Map<UUID, BookRankingCalculation> calculateBookRankingByReviews(List<Review> reviews) {
		return reviews.stream()
			.collect(Collectors.groupingBy(
				review -> review.getBook().getId(),
				Collectors.collectingAndThen(
					Collectors.toList(),
					reviewList -> {
						BigDecimal avgRating = reviewList.stream()
							.map(Review::getRating)
							.reduce(BigDecimal.ZERO, BigDecimal::add)
							.divide(BigDecimal.valueOf(reviewList.size()), 2, RoundingMode.HALF_UP);

						int reviewCount = reviewList.size();

						// 점수 계산: (reviewCount * 0.4) + (avgRating * 0.6)
						BigDecimal weightedCount = BigDecimal.valueOf(reviewCount).multiply(BigDecimal.valueOf(0.4));
						BigDecimal weightedRating = avgRating.multiply(BigDecimal.valueOf(0.6));
						BigDecimal score = weightedCount.add(weightedRating); //최종 도서 스코어

						return new BookRankingCalculation(score, reviewCount, avgRating);
					}
				)
			));
	}

	//도서랭킹 테이블을 모두삭제한다.
	//배치작업시 수행된다.
	public void deleteBookRanking() {
		popularBookRepository.deleteAll();
	}

	//TODO: 도서 리뷰 업데이트(리뷰서비스에서 호출? )

}
