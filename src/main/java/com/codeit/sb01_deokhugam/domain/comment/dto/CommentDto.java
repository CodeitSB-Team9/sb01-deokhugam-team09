package com.codeit.sb01_deokhugam.domain.comment.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentDto(
        UUID id,
        UUID reviewId,
        UUID userId,
        String userNickname,
        String content,
        Instant createdAt,
        Instant updatedAt
) {}
