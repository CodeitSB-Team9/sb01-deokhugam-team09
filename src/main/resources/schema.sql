DROP TABLE IF EXISTS review_rankings;
DROP TABLE IF EXISTS user_rankings;
DROP TABLE IF EXISTS book_rankings;
DROP TABLE IF EXISTS review_likes;
DROP TABLE IF EXISTS comments;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS users;

-- 테이블
-- user
create table users
(
    id         uuid PRIMARY KEY,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    email      varchar(100) UNIQUE      NOT NULL,
    nickname   varchar(50)              NOT NULL,
    password   varchar(60)              NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE    NOT NULL
);

-- notifications
create table notifications
(
    id           uuid PRIMARY KEY,
    created_at   timestamp with time zone NOT NULL,
    updated_at   timestamp with time zone,
    content      varchar                  NOT NULL,
    is_confirmed BOOLEAN                  NOT NULL default false,
    user_id      uuid                     NOT NULL,
    review_id    uuid                     NOT NULL
);

-- comment
create table comments
(
    id         uuid PRIMARY KEY,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone,
    content    varchar                  NOT NULL,
    is_deleted BOOLEAN DEFAULT false    NOT NULL,
    review_id  uuid,
    user_id    uuid
);

-- book
create table books
(
    id             uuid PRIMARY KEY,
    created_at     timestamp with time zone NOT NULL,
    updated_at     timestamp with time zone,
    title          varchar                  NOT NULL,
    author         varchar                  NOT NULL,
    description    varchar                  NOT NULL,
    publisher      varchar                  NOT NULL,
    published_date date                     NOT NULL,
    isbn           varchar                  NOT NULL,
    thumbnail_url  varchar                  NOT NULL,
    review_count   integer                  NOT NULL default 0,
    rating         decimal(2, 1)            NOT NULL,
    is_deleted     BOOLEAN                  NOT NULL default false
);

-- review
create table reviews
(
    id            uuid PRIMARY KEY,
    created_at    timestamp with time zone NOT NULL,
    updated_at    timestamp with time zone,
    content       varchar                  NOT NULL,
    rating        decimal(2, 1)            NOT NULL,
    like_count    integer                  NOT NULL default 0,
    is_deleted    BOOLEAN                  NOT NULL default FALSE,
    comment_count integer                  NOT NULL default 0,
    user_id       uuid,
    book_id       uuid
);

-- review_likes
create table review_likes
(
    id         uuid PRIMARY KEY,
    created_at timestamp with time zone NOT NULL,
    user_id    uuid,
    review_id  uuid,
    UNIQUE (user_id, review_id)
);

CREATE INDEX idx_review_likes_user_review
    ON review_likes (user_id, review_id);

-- book_rankings
create table book_rankings
(
    id            uuid PRIMARY KEY,
    created_at    timestamp with time zone NOT NULL,
    period        varchar(20)              NOT NULL,
    rank          integer                  NOT NULL,
    score         decimal(10, 2)           NOT NULL,
    review_count  integer                  NOT NULL,
    rating        decimal(10, 2)           NOT NULL,
    thumbnail_url varchar                  NOT NULL,
    title         varchar                  NOT NULL,
    book_id       uuid                     NOT NULL,
    author        varchar                  NOT NULL
);

-- user_ranking
create table user_rankings
(
    id               uuid PRIMARY KEY,
    created_at       timestamp with time zone NOT NULL,
    period           varchar(20)              NOT NULL,
    rank             integer                  NOT NULL,
    score            decimal(10, 2)           NOT NULL,
    like_count       integer                  NOT NULL,
    comment_count    integer                  NOT NULL,
    review_score_sum decimal(10, 2)           NOT NULL,
    nickname         varchar                  NOT NULL,
    user_id          uuid                     NOT NULL
);

-- 인기 리뷰 랭킹
create table review_rankings
(
    id                 uuid PRIMARY KEY,
    review_id          uuid                     NOT NULL,
    book_id            uuid                     NOT NULL,
    book_title         varchar                  NOT NULL,
    book_thumbnail_url varchar                  NOT NULL,
    user_id            uuid                     NOT NULL,
    user_nickname      varchar                  NOT NULL,
    review_rating      decimal(2, 1)            NOT NULL,
    period             varchar                  NOT NULL,
    created_at         timestamp with time zone NOT NULL,
    rank               integer                  NOT NULL,
    score              decimal(2, 1)            NOT NULL,
    like_count         integer                  NOT NULL,
    comment_count      integer                  NOT NULL
);

CREATE INDEX idx_review_rankings_period_rank
    ON review_rankings (period, rank);

CREATE INDEX idx_review_rankings_period_created_at
    ON review_rankings (period, created_at);

-- 제약 조건
-- Foreign key

-- User (1) -> notifications (N)
ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_users
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE SET NULL;

-- Review (1) -> notifications (N)
ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_reviews
        FOREIGN KEY (review_id)
            REFERENCES reviews (id)
            ON DELETE CASCADE;

-- comment (N) -> review (1)
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_reviews
        FOREIGN KEY (review_id)
            REFERENCES reviews (id)
            ON DELETE CASCADE;

-- comment (N) -> users (1)
ALTER TABLE comments
    ADD CONSTRAINT fk_comments_users
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE SET NULL;

-- review (N) -> users (1)
ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_users
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE SET NULL;

-- review (N) -> books (1)
ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_books
        FOREIGN KEY (book_id)
            REFERENCES books (id)
            ON DELETE CASCADE;

-- review_likes
-- review_likes (N) -> users (1)
ALTER TABLE review_likes
    ADD CONSTRAINT fk_review_likes_users
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE SET NULL;

-- review_likes (N) -> reviews (1)
ALTER TABLE review_likes
    ADD CONSTRAINT fk_review_likes_reviews
        FOREIGN KEY (review_id)
            REFERENCES reviews (id)
            ON DELETE CASCADE;
