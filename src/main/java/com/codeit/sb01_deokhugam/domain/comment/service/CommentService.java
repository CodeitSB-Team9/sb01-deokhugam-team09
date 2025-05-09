package com.codeit.sb01_deokhugam.domain.comment.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.codeit.sb01_deokhugam.domain.comment.dto.CommentDto;
import com.codeit.sb01_deokhugam.domain.comment.dto.CommentResponse;
import com.codeit.sb01_deokhugam.domain.comment.entity.Comment;
import com.codeit.sb01_deokhugam.domain.comment.exception.CommentException;
import com.codeit.sb01_deokhugam.domain.comment.mapper.CommentMapper;
import com.codeit.sb01_deokhugam.domain.comment.repository.CommentRepository;
import com.codeit.sb01_deokhugam.domain.notification.entity.Notification;
import com.codeit.sb01_deokhugam.domain.notification.repository.NotificationRepository;
import com.codeit.sb01_deokhugam.domain.review.entity.Review;
import com.codeit.sb01_deokhugam.domain.review.repository.ReviewRepository;
import com.codeit.sb01_deokhugam.domain.user.entity.User;
import com.codeit.sb01_deokhugam.domain.user.repository.UserRepository;
import com.codeit.sb01_deokhugam.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    public CommentDto create(UUID reviewId, UUID userId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CommentException(ErrorCode.USER_NOT_FOUND));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CommentException(ErrorCode.REVIEW_NOT_FOUND));
        Comment comment = new Comment(review, user, content);
        Comment saved = commentRepository.save(comment);

        Notification notification = Notification.fromComment(user, content, review);
        notificationRepository.save(notification);
        
        String nickname = user.getNickname();
      	review.incrementCommentCount();


        return commentMapper.toDto(saved, nickname);
    }

    public CommentResponse getComments(UUID reviewId, Instant after, String direction, String cursor, Integer limit) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new CommentException(ErrorCode.REVIEW_NOT_FOUND);
        }

        boolean isAsc = "ASC".equalsIgnoreCase(direction);
        int pageSize = (limit != null) ? limit : 50;

        Instant cursorCreatedAt = null;
        if (cursor != null) {
            UUID commentId = UUID.fromString(cursor);
            Comment cursorComment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new CommentException(ErrorCode.COMMENT_NOT_FOUND));
            cursorCreatedAt = cursorComment.getCreatedAt();
        }

        Instant afterTime = (after != null) ? after : Instant.EPOCH;
        Instant beforeTime = (cursorCreatedAt != null) ? cursorCreatedAt : Instant.parse("9999-12-31T23:59:59Z"); // Java와 PostgreSQL 모두 허용하는 법위

        List<Comment> comments = commentRepository.findComments(
                reviewId,
                afterTime,
                beforeTime,
                isAsc,
                pageSize + 1
        );

        boolean hasNext = comments.size() > pageSize;
        if (hasNext) {
            comments = comments.subList(0, pageSize);
        }

        String nextCursor = hasNext ? comments.get(comments.size() - 1).getId().toString() : null;
        Instant nextAfter = hasNext ? comments.get(comments.size() - 1).getCreatedAt() : null;

        List<CommentDto> content = comments.stream()
                .map(comment -> {
                    String nickname = comment.getUser().getNickname();
                    return commentMapper.toDto(comment, nickname);
                })
                .toList();

        long totalElements = commentRepository.countByReview_Id(reviewId);

        return new CommentResponse(content, nextCursor, nextAfter, pageSize, totalElements, hasNext);
    }

    public CommentDto getCommentById(UUID commentId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CommentException(ErrorCode.COMMENT_NOT_FOUND));

        String nickname = comment.getUser().getNickname();

        return commentMapper.toDto(comment, nickname);
    }

    @Transactional
    public CommentDto updateComment(UUID commentId, UUID userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(ErrorCode.NOT_AUTHORITY);
        }

        comment.updateContent(content);

        String nickname = comment.getUser().getNickname();

        return commentMapper.toDto(comment, nickname);
    }

    @Transactional
    public void softDelete(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CommentException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(ErrorCode.ACCESS_DENIED);
        }
      	Review review = reviewRepository.findById(comment.getReviewId()).get();
	review.decrementCommentCount();
        comment.markDeleted();
    }

    @Transactional
    public void hardDelete(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(ErrorCode.ACCESS_DENIED);
        }

        if (comment.isDeleted()) {
            throw new CommentException(ErrorCode.INVALID_REQUEST);
        }
	Review review = reviewRepository.findById(comment.getReviewId()).get();
	review.decrementCommentCount();	    
        commentRepository.deleteById(commentId);
    }
}
