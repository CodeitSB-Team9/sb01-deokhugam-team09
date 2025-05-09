package com.codeit.sb01_deokhugam.domain.book.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import com.codeit.sb01_deokhugam.domain.book.dto.BookCreateRequest;
import com.codeit.sb01_deokhugam.domain.book.dto.BookDto;
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
import com.codeit.sb01_deokhugam.domain.user.entity.User;
import com.codeit.sb01_deokhugam.global.dto.response.PageResponse;
import com.codeit.sb01_deokhugam.global.enumType.Period;
import com.codeit.sb01_deokhugam.global.naver.NaverBookClient;
import com.codeit.sb01_deokhugam.global.s3.S3Service;
import com.codeit.sb01_deokhugam.global.schedule.utils.ScheduleUtils;
import com.codeit.sb01_deokhugam.util.EntityProvider;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private PopularBookRepository popularBookRepository;

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private S3Service s3SeService;

	@Mock
	private BookMapper bookMapper;

	@Mock
	private PopularBookMapper popularBookMapper;

	@Mock
	private Tesseract tesseract;

	@Mock
	private NaverBookClient naverBookClient;

	@Captor
	private ArgumentCaptor<List<BookRanking>> bookRankingsCaptor; //인자의 값을 확인할 수 있는 Captor

	@InjectMocks
	private BookService bookService;

	private BookCreateRequest bookCreateRequest;
	private Book book1;
	private Book book2;
	private Book book3;
	private Book book4;
	private Book mockBook;//Mock으로 만든 Book 객체, verify에서 사용된다.
	private BookDto bookDto1;
	private BookDto bookDto2;
	private BookDto bookDto4;
	private MultipartFile multipartFile;
	private Instant created;
	private Instant updated;
	private UUID bookId1;
	private UUID bookId2;
	private UUID bookId3;
	private UUID bookId4;
	private UUID mockbookId1;
	private Review review1;
	private Review review2;
	private Review review3;
	private Review review4;

	@BeforeEach
	void setUp() {

		created = Instant.now();
		updated = Instant.now();
		bookId1 = UUID.randomUUID();
		bookId2 = UUID.randomUUID();
		bookId3 = UUID.randomUUID();
		bookId4 = UUID.randomUUID();
		mockbookId1 = UUID.randomUUID();
		mockBook = Mockito.mock(Book.class);

		bookCreateRequest = new BookCreateRequest(
			"저자",
			"책입니다.",
			"12345678",
			LocalDate.parse("2025-01-01"),
			"출판사",
			"제목"
		);

		book1 = new Book(
			"제목", "저자", "책입니다.", "12345678",
			"출판사", LocalDate.parse("2025-01-01"),
			"https://test.com",
			0, new BigDecimal("5.0"), false
		);
		//book1의 id를 bookId1로 세팅
		ReflectionTestUtils.setField(book1, "id", bookId1);

		book2 = new Book(
			"책 제목 2", "저자 2", "설명 2", "23456789",
			"출판사 2", LocalDate.parse("2025-01-02"),
			"https://test2.com",
			0, new BigDecimal("4.5"), false
		);
		ReflectionTestUtils.setField(book2, "id", bookId2);

		book3 = new Book(
			"책 제목 3", "저자 3", "설명 3", "34567890",
			"출판사 3", LocalDate.parse("2025-01-03"),
			"https://test3.com",
			0, new BigDecimal("4.0"), true
		);
		ReflectionTestUtils.setField(book3, "id", bookId3);

		book4 = new Book(
			"책 제목 4", "저자 4", "설명 4", "44467890",
			"출판사 4", LocalDate.parse("2025-01-04"),
			"https://test4.com",
			0, new BigDecimal("4.0"), false
		);
		ReflectionTestUtils.setField(book4, "id", bookId4);

		bookDto1 = new BookDto(
			"저자", created,
			"책입니다.", bookId1,
			"12345678", LocalDate.parse("2025-01-01"), "출판사",
			new BigDecimal("5.0"), 0, "https://test.com", "제목", updated
		);
		bookDto2 = new BookDto(
			"저자 2", created,
			"설명 2", bookId2,
			"23456789", LocalDate.parse("2025-01-02"), "출판사 2",
			new BigDecimal("4.5"), 0, "https://test2.com", "책 제목 2", updated
		);
		bookDto4 = new BookDto(
			"저자 4", created,
			"설명 4", bookId4,
			"44467890", LocalDate.parse("2025-01-04"), "출판사 4",
			new BigDecimal("4.0"), 0, "https://test4.com", "책 제목4", updated
		);

		multipartFile = Mockito.mock(MultipartFile.class);

		User user = EntityProvider.createUser();

		// 리뷰 객체 생성.
		review1 = EntityProvider.createReviewByRating(user, book1, new BigDecimal("5.0"));
		ReflectionTestUtils.setField(review1, "createdAt", Instant.parse("2025-01-01T12:00:00Z"));

		review2 = EntityProvider.createReviewByRating(user, book2, new BigDecimal("5.0"));
		ReflectionTestUtils.setField(review2, "createdAt", Instant.parse("2025-01-01T12:00:00Z"));

		//논리적 삭제된 책에 대한 리뷰
		review3 = EntityProvider.createReviewByRating(user, book3, new BigDecimal("4.0"));
		ReflectionTestUtils.setField(review3, "createdAt", Instant.parse("2025-01-01T12:00:00Z"));

		review4 = EntityProvider.createReviewByRating(user, book4, new BigDecimal("3.0"));
		ReflectionTestUtils.setField(review4, "createdAt", Instant.parse("2025-01-01T12:00:00Z"));

	}

	@Nested
	@DisplayName("도서 등록 테스트")
	class testAddBook {

		@Test
		@DisplayName("도서 등록을 성공한다.")
		void testAddBook_ReturnsBook() {

			//given
			given(bookRepository.existsByIsbn(eq(book1.getIsbn()))).willReturn(false);
			given(s3SeService.upload(any(MultipartFile.class), eq("QBook")))
				.willReturn("https://test.com");
			given(bookRepository.save(any(Book.class))).willReturn(book1);
			given(bookMapper.toDto(any(Book.class))).willReturn(bookDto1);

			// when
			BookDto result = bookService.create(bookCreateRequest, multipartFile);

			//then
			//S3에 이미지가 업로드된다.
			verify(s3SeService).upload(any(MultipartFile.class), eq("QBook"));
			//DB에 도서가 저장된다.
			verify(bookRepository).save(any(Book.class));

			assertEquals(bookDto1, result);
		}

		//중복 isbn 체크
		@Test
		@DisplayName("중복된 isbn 등록을 시도하면 도서등록을 실패한다.")
		void testAddBook_FailedCauseExistsIsbn() {

			given(bookRepository.existsByIsbn(eq(book1.getIsbn()))).willReturn(true);

			// when & then
			assertThrows(IsbnAlreadyExistsException.class, () -> {
				bookService.create(bookCreateRequest, multipartFile);
			});

			// 저장 메서드는 호출되지 않아야 한다
			verify(bookRepository, never()).save(any(Book.class));
		}

	}

	@Nested
	@DisplayName("도서 목록 조회 테스트")
	class testSearchBooks {
		private String keyword = null;
		private Instant after = null;
		private String cursor = null;
		private String orderBy = "title";
		private String direction = "desc";
		private int limit = 2;
		private List<Book> books; //도서 목록 조회 bookRepository.findListByCursor에서 반환되는 책리스트

		@BeforeEach
		void setUp() {
			// 논리적 삭제된 book3은 포함하지않음
			books = Arrays.asList(book4, book2, book1);
		}

		@Test
		@DisplayName("커서와 after 없이 내림차순 도서 목록 조회를 성공한다.")
		void testSearchBooks_withoutCursorAndAfter_returnsBooks() {
			//given
			given(bookRepository.findListByCursor(keyword, after, cursor, orderBy, direction, limit + 1)).willReturn(
				books);
			given(bookMapper.toDto(book4)).willReturn(bookDto4);
			given(bookMapper.toDto(book2)).willReturn(bookDto2);
			given(bookRepository.getTotalElements(keyword)).willReturn((long)3);

			//when
			PageResponse<BookDto> response = bookService.findAllWithCursor(keyword, after, cursor, orderBy, direction,
				limit);

			//then
			assertEquals(2, response.getContent().size());
			//내림차순 정렬되었는가
			assertEquals(bookDto4, response.getContent().get(0));
			assertThat(response.isHasNext()).isTrue();
			assertThat(response.getNextCursor()).isEqualTo(book2.getTitle());

		}

		@Test
		@DisplayName("유효하지 않은 정렬 기준을 입력하여 도서 목록 조회를 실패한다.")
		void testSearchBooks_withInvalidOrderBy_returnsFail() {
			//given
			orderBy = "invalid";

			//when&then
			assertThrows(IllegalArgumentException.class, () -> {
				bookService.findAllWithCursor(keyword, after, cursor, orderBy, direction, limit);
			});
		}
	}

	@Nested
	@DisplayName("인기 도서 목록 조회 테스트")
	class testSearchPopularBooks {
		private Period period = Period.DAILY;
		private String periodString = "DAILY";
		private Instant after = null;
		private String cursor = null;
		private String direction = "desc";
		private int limit = 2;
		private List<BookRanking> bookRankings; //
		private BookRanking bookRanking1;
		private BookRanking bookRanking2;
		private BookRanking bookRanking4;
		private PopularBookDto popularBookDto1;
		private PopularBookDto popularBookDto2;
		private PopularBookDto popularBookDto4;

		@BeforeEach
		void setUp() {
			//논리삭제된 도서(book3)는 제외
			bookRanking1 = EntityProvider.createBookRanking(period, 1, new BigDecimal("7.5"), 2,
				new BigDecimal("5.0"), bookId1);
			bookRanking2 = EntityProvider.createBookRanking(period, 1, new BigDecimal("7.5"), 4,
				new BigDecimal("3.0"), bookId2);
			bookRanking4 = EntityProvider.createBookRanking(period, 3, new BigDecimal("4.5"), 2,
				new BigDecimal("2.0"), bookId4);
			bookRankings = Arrays.asList(bookRanking1, bookRanking2, bookRanking4);

			popularBookDto1 = EntityProvider.createPopularBookDto(bookId1, period, 1, 7.5, 2, new BigDecimal("5.0"));
			popularBookDto2 = EntityProvider.createPopularBookDto(bookId2, period, 1, 7.5, 4, new BigDecimal("3.0"));
			popularBookDto4 = EntityProvider.createPopularBookDto(bookId4, period, 3, 4.5, 2, new BigDecimal("2.0"));

		}

		@Test
		@DisplayName("DAILY 조회 기준으로 도서 랭킹을 조회한다.")
		void testSearchPopularBooks_returnsBooks() {
			given(popularBookRepository.findListByCursor(periodString, after, cursor, direction, limit + 1)).willReturn(
				bookRankings);
			given(popularBookMapper.toDto(bookRanking1)).willReturn(popularBookDto1);
			given(popularBookMapper.toDto(bookRanking2)).willReturn(popularBookDto2);
			given(popularBookRepository.getTotalElements(periodString)).willReturn(3L);

			//when
			PageResponse<PopularBookDto> response = bookService.findPopularBook(periodString, after, cursor, direction,
				limit);

			//then
			assertEquals(2, response.getContent().size());
			assertEquals(popularBookDto1, response.getContent().get(0));
			assertThat(response.getNextCursor()).isEqualTo("1");

		}

		@Test
		@DisplayName("Period가 enum에 속하지 않으면 예외를 발생한다.")
		void testSearchPopularBooks_invalidPeriod_returnsFail() {
			//given
			periodString = "invalidPeriod";

			//when&then
			assertThrows(IllegalArgumentException.class, () -> {
				bookService.findPopularBook(periodString, after, cursor, direction, limit);
			});
		}
	}

	@Nested
	@DisplayName("도서 상세 조회 테스트")
	class testFindBook {
		@Test
		@DisplayName("bookId1에 대한 도서 조회를 성공한다.")
		void testFindBook_returnsBook() {
			//given
			given(bookRepository.findByIdNotLogicalDelete(bookId1)).willReturn(Optional.of(book1));
			given(bookMapper.toDto(any(Book.class))).willReturn(bookDto1);

			//when
			BookDto result = bookService.findById(bookId1);

			//then'
			assertEquals(bookDto1, result);

		}

		@Test
		@DisplayName("존재하지 않는 bookId1에 대하여 예외를 발생한다.")
		void testFindBook_returnsException() {
			given(bookRepository.findByIdNotLogicalDelete(bookId1)).willReturn(Optional.empty());

			assertThrows(BookNotFoundException.class, () -> {
				bookService.findById(bookId1);
			});

			verify(bookRepository, never()).save(any(Book.class));

		}
	}

	@Nested
	@DisplayName("도서 논리 삭제 테스트")
	class testLogicalDeleteBook {
		@Test
		@DisplayName("도서를 논리적으로 삭제 성공한다.")
		void testLogicalDeleteBook_success() {
			//given
			given(bookRepository.findByIdNotLogicalDelete(mockbookId1)).willReturn(Optional.of(mockBook));

			//when
			bookService.delete(mockbookId1);

			//then
			//softDelete()가 호출되었는지 검증한다.
			verify(mockBook, times(1)).softDelete();
		}

		@Test
		@DisplayName("존재하지 않는 도서의 논리적 삭제 시도를 실패한다.")
		void testLogicalDeleteBook_failed() {
			//given
			given(bookRepository.findByIdNotLogicalDelete(bookId1)).willReturn(Optional.empty());

			//when & then
			assertThrows(BookNotFoundException.class, () -> bookService.delete(bookId1));
		}

	}

	@Nested
	@DisplayName("도서 물리 삭제 테스트")
	class testPysicallDeleteBook {
		@Test
		@DisplayName("도서를 물리적으로 삭제 성공한다.")
		void testPysicalDeleteBook_success() {
			//given
			given(bookRepository.findById(bookId1)).willReturn(Optional.of(book1));

			//when
			bookService.deletePhysical(bookId1);

			//then
			verify(bookRepository).deleteById(bookId1);
		}

		@Test
		@DisplayName("존재하지 않는 도서의 물리적 삭제 시도를 실패한다.")
		void testPysicalDeleteBook_failed() {
			//given
			given(bookRepository.findById(bookId1)).willReturn(Optional.empty());

			//when & then
			assertThrows(BookNotFoundException.class, () -> bookService.deletePhysical(bookId1));
		}

	}

	@Nested
	@DisplayName("도서 수정 테스트")
	class testUpdateBook {
		@Test
		@DisplayName("도서의 정보 수정을 성공한다.")
		void testUpdateBook_returnsBook() {
			//given
			BookUpdateRequest bookUpdateRequest = new BookUpdateRequest("새로운 제목", "새로운 작가", "새로운 설명", "새로운 출판사",
				LocalDate.parse("2025-05-05"));
			given(bookRepository.findByIdNotLogicalDelete(mockbookId1)).willReturn(Optional.of(mockBook));
			given(s3SeService.upload(any(MultipartFile.class), eq("QBook")))
				.willReturn("https://test.com");

			//when
			bookService.update(mockbookId1, bookUpdateRequest, multipartFile);

			//then
			verify(mockBook).update("새로운 제목", "새로운 작가", "새로운 설명", "새로운 출판사",
				LocalDate.parse("2025-05-05"), "https://test.com");

		}

		@Test
		@DisplayName("유효하지 않은 도서의 정보 수정 시도에 대해 실패한다.")
		void testUpdateBook_failed() {
			//given
			BookUpdateRequest bookUpdateRequest = new BookUpdateRequest("새로운 제목", "새로운 작가", "새로운 설명", "새로운 출판사",
				LocalDate.parse("2025-05-05"));
			given(bookRepository.findByIdNotLogicalDelete(mockbookId1)).willReturn(Optional.empty());

			//when&then
			assertThrows(BookNotFoundException.class,
				() -> bookService.update(mockbookId1, bookUpdateRequest, multipartFile));
		}
	}

	@Nested
	@DisplayName("OCR 테스트")
	class testExtractTextByOcr {

		@Test
		@DisplayName("이미지를 OCR로 읽어들여 isbn을 반환을 성공한다.")
		void testExtractTextByOcr_returnsIsbn() throws IOException, TesseractException {
			//given
			String ocrText = "ISBN 97889-123-4567-8"; // OCR 결과에 잡히는 텍스트
			String expectedIsbn = "9788912345678";
			BufferedImage mockImage = mock(BufferedImage.class); // OCR 수행을 위한 더미 이미지 객체 생성
			InputStream inputStream = new ByteArrayInputStream(new byte[1]);

			given(multipartFile.isEmpty()).willReturn(false);
			given(multipartFile.getInputStream()).willReturn(inputStream);

			try (MockedStatic<ImageIO> imageIo = mockStatic(ImageIO.class)) {
				imageIo.when(() -> ImageIO.read(inputStream)).thenReturn(mockImage);
				when(tesseract.doOCR(mockImage)).thenReturn(ocrText);

				//when
				String result = bookService.extractTextByOcr(multipartFile);

				// then
				assertEquals(expectedIsbn, result);
			}

		}

	}

	@Nested
	@DisplayName("인기 도서 배치 연산 테스트")
	class testCalculateBookRanking {

		private Instant start;
		private Instant end;
		private Period period;
		private List<Review> reviews;
		private List<Book> bookList;

		@BeforeEach
		public void setUp() {
			start = Instant.parse("2025-01-01T00:00:00Z");
			end = Instant.parse("2025-01-01T23:59:59Z");
			period = Period.DAILY;
			reviews = Arrays.asList(review1, review2, review3, review4);
			;
			bookList = List.of(book1, book2, book4);

		}

		@Test
		@DisplayName("인기 도서 배치 연산을 성공하고 저장한다. 동일한 도서 스코어를 가진 책을 동일한 등수로 처리한다. 논리적 삭제된 책은 연산에 포함하지 않는다.")
		void testCalculateBookRanking() {
			//static
			try (MockedStatic<ScheduleUtils> mockedStatic = mockStatic(ScheduleUtils.class)) {
				mockedStatic.when(() -> ScheduleUtils.getStartAndEndByPeriod(period)).thenReturn(Map.entry(start, end));

				//given
				when(reviewRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class))).thenReturn(
					reviews);
				;
				when(bookRepository.findAllById(anySet())).thenReturn(bookList);

				//when
				bookService.calculateRanking(period);

				// then
				verify(popularBookRepository).saveAll(bookRankingsCaptor.capture());
				List<BookRanking> capturedRankings = bookRankingsCaptor.getValue();

				// 동일한 점수를 가진 두 도서의 랭킹 확인 (첫 번째와 두 번째가 같은 랭킹이어야 함)
				assertEquals(capturedRankings.get(0).getRank(), capturedRankings.get(1).getRank());

				// 3번째 책은 3등이다.
				assertEquals(capturedRankings.get(2).getRank(), 3);

				// 논리적 삭제된 책은 랭킹에 포함되지 않는다.
				assertEquals(capturedRankings.size(), 3);

			}

		}
	}

	@Nested
	@DisplayName("도서 랭킹 삭제 테스트")
	class testDeleteBookRanking {

		@Test
		@DisplayName("도서 랭킹 삭제를 성공한다.")
		public void testDeleteBookRanking_returnsEmpty() {
			// given
			willDoNothing().given(popularBookRepository).deleteAll();

			//when
			bookService.deleteBookRanking();

			//then
			verify(popularBookRepository, times(1)).deleteAll();
		}
	}

	// TODO: global로 빼기
	// //Naver API를 통한 ISBN 책 정보 불러오기
	// @Test
	// @DisplayName("네이버 API - ISBN 조회 테스트")
	// void testNaverBookApi() {
	//
	// 	//given
	// 	String isbn = "1234567890";
	// 	UUID uuid = UUID.randomUUID();
	// 	BookDto bookDto = new BookDto(
	// 		"작가",
	// 		Instant.now(),
	// 		"네이버 도서 api를 이용해서 로드한  책입니다. ",
	// 		uuid,
	// 		"1234567890",
	// 		LocalDate.parse("2025-01-01"),
	// 		"출판사",
	// 		new BigDecimal("5.0"),
	// 		0,
	// 		"https://www.naver.com",
	// 		"제목",
	// 		Instant.now()
	//
	// 	);
	// 	//
	// 	// //when
	// 	// /when(naverBookClient.)
	//
	// }

}
