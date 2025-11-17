-- 유저 테이블
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE COMMENT '실제 로그인에 사용할 ID',
    point_balance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '현재 포인트 잔액',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 카테고리 테이블
CREATE TABLE categories (
    id BIGINT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE COMMENT '카테고리 이름',
    display_order INT DEFAULT 0 COMMENT '카테고리 노출 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 상품 테이블
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    category_id BIGINT,
    name VARCHAR(200) NOT NULL COMMENT '상품명',
    description TEXT COMMENT '상품 설명',
    price DECIMAL(10,2) NOT NULL COMMENT '상품 가격',
    stock INT NOT NULL DEFAULT 0 COMMENT '재고 수량',
    is_active BOOLEAN DEFAULT TRUE COMMENT '활성화 여부',
    view_count INT DEFAULT 0 COMMENT '조회수 (인기순 정렬용)',
    sold_count INT DEFAULT 0 COMMENT '판매량 (인기순 정렬용)',
    min_order_quantity INT DEFAULT 1 COMMENT '최소 구매 수량',
    max_order_quantity INT COMMENT '최대 구매 수량',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,

    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 장바구니 테이블
CREATE TABLE carts (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1 COMMENT '수량',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 포인트 테이블
CREATE TABLE points (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL COMMENT '포인트 적립/사용 금액',
    used_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '사용된 금액',
    point_type VARCHAR(50) NOT NULL COMMENT '포인트 유형 (CHARGE, REFUNDED)',
    description VARCHAR(255) COMMENT '적립/사용 사유',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expired_at TIMESTAMP NULL COMMENT '포인트 만료일',
    used_at TIMESTAMP NULL COMMENT '포인트 사용일',
    deleted_at TIMESTAMP NULL COMMENT '소프트 삭제일',
    is_expired TINYINT(1) DEFAULT 0 COMMENT '만료 여부',
    is_used TINYINT(1) DEFAULT 0 COMMENT '사용 여부',

    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 쿠폰 마스터 테이블
CREATE TABLE coupons (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '쿠폰 이름 ("신규가입 쿠폰", "5월 할인 이벤트")',
    code VARCHAR(100) UNIQUE COMMENT '쿠폰 코드 ("WELCOME2025")',
    discount_type VARCHAR(50) NOT NULL COMMENT '할인 타입 (PERCENTAGE, FIXED)',
    discount_value DECIMAL(10,2) NOT NULL COMMENT '할인 값 (10% 또는 10,000원)',
    max_discount_amount DECIMAL(10,2) COMMENT '최대 할인 금액 (% 쿠폰일 때)',
    min_order_amount DECIMAL(10,2) COMMENT '최소 주문 금액',
    total_quantity INT COMMENT '총 발급 가능 수량 (NULL이면 무제한)',
    issued_quantity INT DEFAULT 0 COMMENT '발급된 수량',
    per_user_limit INT DEFAULT 1 COMMENT '1인당 사용 가능 횟수',
    start_date TIMESTAMP NOT NULL COMMENT '시작 시점',
    end_date TIMESTAMP NOT NULL COMMENT '종료 시점',
    is_active TINYINT(1) DEFAULT 1 COMMENT '사용 가능 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 발급된 쿠폰 테이블
CREATE TABLE user_coupons (
    id BIGINT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE' COMMENT '쿠폰 상태 (AVAILABLE, USED, EXPIRED)',
    used_count INT NOT NULL DEFAULT 0 COMMENT '사용 횟수',
    used_at TIMESTAMP NULL COMMENT '사용 시간',
    expired_at TIMESTAMP NULL COMMENT '만료 시간',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '발급 시간',

    -- 외래키
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주문 테이블
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NULL COMMENT '사용된 쿠폰',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '상품 금액 합계',
    discount_amount DECIMAL(10,2) DEFAULT 0 COMMENT '할인 금액 (쿠폰)',
    shipping_fee DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '배송비',
    final_amount DECIMAL(10,2) NOT NULL COMMENT '최종 결제 금액 = total_amount - discount_amount + shipping_fee - point_amount',
    status VARCHAR(50) NOT NULL COMMENT '주문 상태 (PENDING, PAID, PAYMENT_FAILED, CANCELED)',
    point_amount DECIMAL(10,2) DEFAULT 0 COMMENT '사용된 포인트',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL COMMENT '결제 완료 시각',
    canceled_at TIMESTAMP NULL COMMENT '주문 취소 시각',

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주문 항목 테이블
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL COMMENT '주문 당시 상품명',
    quantity INT NOT NULL COMMENT '주문 수량',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '단가',
    subtotal DECIMAL(10,2) NOT NULL COMMENT 'quantity * unit_price',
    status VARCHAR(50) NOT NULL COMMENT '주문 상태 (ORDER_PENDING, ORDER_COMPLETED, ORDER_CANCELED, ORDER_RETURNED, ORDER_REFUNDED, PURCHASE_CONFIRMED)',
    confirmed_at TIMESTAMP NULL COMMENT '구매 확정 시각',
    canceled_at TIMESTAMP NULL COMMENT '주문 취소 시각',
    returned_at TIMESTAMP NULL COMMENT '반품 시각',
    refunded_at TIMESTAMP NULL COMMENT '환불 시각',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 외래키
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 결제 테이블
CREATE TABLE payments (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL COMMENT '결제 금액',
    payment_type VARCHAR(50) NOT NULL COMMENT '결제 유형 (PAYMENT, REFUND)',
    payment_method VARCHAR(50) NOT NULL COMMENT '결제 수단 (CARD, BANK_TRANSFER, KAKAO_PAY, TOSS 등)',
    payment_status VARCHAR(50) NOT NULL COMMENT '결제 상태 (PENDING, COMPLETED, FAILED, REFUNDED)',
    failure_reason TEXT COMMENT '결제 실패 사유',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL COMMENT '결제 완료 시각',
    failed_at TIMESTAMP NULL COMMENT '결제 실패 시각',

    -- 외래키
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 포인트 사용 이력 테이블
CREATE TABLE point_usage_histories (
    id BIGINT PRIMARY KEY,
    point_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    used_amount DECIMAL(10,2) NOT NULL COMMENT '사용한 포인트 금액',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at TIMESTAMP NULL COMMENT '포인트 사용 취소 시각',

    -- 외래키
    FOREIGN KEY (point_id) REFERENCES points(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
