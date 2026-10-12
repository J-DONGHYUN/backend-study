-- =====================================================================
-- SQL 연습용 공통 스키마 — 쇼핑몰 도메인
-- 면접 SQL 문제의 단골 구조다. 한 번 만들어두고 계속 쓴다.
--
--   member ──< orders ──< order_item >── product
--
-- 적재:  docker exec -i sql-lab mysql -uroot -plabpass shop < cs/db/_schema.sql
-- =====================================================================

DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS member;

CREATE TABLE member (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(20)  NOT NULL,
    email       VARCHAR(100) NOT NULL UNIQUE,
    grade       VARCHAR(10)  NOT NULL,            -- BRONZE / SILVER / GOLD
    city        VARCHAR(20)  NOT NULL,
    joined_at   DATE         NOT NULL
);

CREATE TABLE product (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    category    VARCHAR(20)  NOT NULL,            -- 전자/의류/식품/도서
    price       INT          NOT NULL,
    stock       INT          NOT NULL
);

CREATE TABLE orders (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id   BIGINT       NOT NULL,
    ordered_at  DATETIME     NOT NULL,
    status      VARCHAR(10)  NOT NULL,            -- PAID / SHIPPED / CANCELED
    CONSTRAINT fk_orders_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE order_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    product_id  BIGINT       NOT NULL,
    quantity    INT          NOT NULL,
    unit_price  INT          NOT NULL,            -- 주문 시점 가격 (현재가와 다를 수 있다)
    CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

-- ------------------------------ 회원 10명 (주문이 없는 회원도 섞여 있다)
INSERT INTO member (name, email, grade, city, joined_at) VALUES
('김동현','donghyun@test.com','GOLD'  ,'서울',  '2024-01-15'),
('이서연','seoyeon@test.com' ,'SILVER','부산',  '2024-03-02'),
('박민준','minjun@test.com'  ,'BRONZE','서울',  '2024-05-20'),
('최지우','jiwoo@test.com'   ,'GOLD'  ,'대구',  '2023-11-08'),
('정하윤','hayoon@test.com'  ,'SILVER','서울',  '2025-01-10'),
('강도윤','doyoon@test.com'  ,'BRONZE','인천',  '2025-02-25'),
('윤서준','seojun@test.com'  ,'GOLD'  ,'부산',  '2023-07-19'),
('장예은','yeeun@test.com'   ,'SILVER','대전',  '2025-04-03'),
('임시우','siwoo@test.com'   ,'BRONZE','서울',  '2025-06-11'),
('한수아','sua@test.com'     ,'BRONZE','광주',  '2025-08-22');

-- ------------------------------ 상품 15개 (한 번도 안 팔린 상품도 있다)
INSERT INTO product (name, category, price, stock) VALUES
('무선 이어폰'   ,'전자',  129000, 50),
('기계식 키보드' ,'전자',   89000, 30),
('27인치 모니터' ,'전자',  329000, 12),
('USB-C 허브'   ,'전자',   45000,  0),
('노트북 스탠드' ,'전자',   32000, 80),
('면 티셔츠'     ,'의류',   19900,200),
('청바지'       ,'의류',   59000, 45),
('패딩 점퍼'     ,'의류',  189000,  8),
('운동화'       ,'의류',   79000, 60),
('아메리카노 원두','식품',   24000,120),
('유기농 꿀'     ,'식품',   38000, 25),
('견과류 세트'   ,'식품',   29900, 70),
('자바의 정석'   ,'도서',   30000, 15),
('클린 코드'     ,'도서',   33000, 22),
('객체지향의 사실','도서',   20000,  0);

-- ------------------------------ 주문 20건
INSERT INTO orders (member_id, ordered_at, status) VALUES
(1,'2025-07-01 10:12:00','PAID'),
(1,'2025-07-15 14:30:00','SHIPPED'),
(1,'2025-08-03 09:05:00','CANCELED'),
(2,'2025-07-05 16:40:00','PAID'),
(2,'2025-09-11 11:20:00','SHIPPED'),
(3,'2025-07-20 13:00:00','PAID'),
(4,'2025-06-18 18:45:00','SHIPPED'),
(4,'2025-08-25 10:00:00','PAID'),
(4,'2025-09-30 20:15:00','PAID'),
(5,'2025-08-09 12:30:00','CANCELED'),
(5,'2025-09-02 15:55:00','PAID'),
(6,'2025-09-14 08:20:00','PAID'),
(7,'2025-06-29 19:10:00','SHIPPED'),
(7,'2025-07-30 17:25:00','PAID'),
(7,'2025-09-21 21:40:00','PAID'),
(8,'2025-08-17 14:05:00','SHIPPED'),
(9,'2025-09-05 10:50:00','PAID'),
(9,'2025-10-01 13:15:00','PAID'),
(2,'2025-10-02 09:30:00','PAID'),
(1,'2025-10-05 11:00:00','PAID');

-- ------------------------------ 주문 상세 40건
INSERT INTO order_item (order_id, product_id, quantity, unit_price) VALUES
(1,1,1,129000),(1,10,2,24000),
(2,2,1,89000),(2,5,1,32000),(2,13,1,30000),
(3,3,1,329000),
(4,6,3,19900),(4,9,1,79000),
(5,7,1,59000),(5,6,2,19900),
(6,10,1,24000),(6,12,2,29900),
(7,3,1,329000),(7,2,1,89000),
(8,14,1,33000),(8,13,1,30000),
(9,1,2,129000),
(10,8,1,189000),
(11,11,1,38000),(11,12,1,29900),(11,10,3,24000),
(12,6,1,19900),
(13,9,2,79000),(13,7,1,59000),
(14,5,2,32000),(14,2,1,89000),
(15,1,1,129000),(15,14,1,33000),(15,13,1,30000),
(16,12,4,29900),
(17,6,5,19900),
(18,9,1,79000),(18,11,2,38000),
(19,2,1,89000),(19,5,1,32000),(19,1,1,129000),
(20,13,2,30000),(20,14,1,33000),(20,10,1,24000),(20,12,1,29900);
