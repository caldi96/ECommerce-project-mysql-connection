package io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CouponException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Coupon extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String code;

    // ===== 할인 정보 =====

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    // ===== 수량 관리 =====

    @Column(name = "total_quantity")
    private int totalQuantity;              // 전체 수량

    @Column(name = "issued_quantity", nullable = false)
    private int issuedQuantity;             // 발급된 양

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit;               // 인당 사용가능 양

    // ===== 유효 기간 =====
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    // ===== 상태 관리 =====
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // ===== 정적 팩토리 메서드 =====

    /**
     * 쿠폰 생성
     */
    public static Coupon createCoupon(
            String name,
            String code,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount,
            Integer totalQuantity,
            Integer perUserLimit,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        validateName(name);
        validateDiscountType(discountType);
        validateDiscountValue(discountValue);
        validateTotalQuantity(totalQuantity);
        validatePerUserLimit(perUserLimit);
        validateDateRange(startDate, endDate);

        // 정률 할인일 경우 할인율 범위 검증 (0 ~ 100)
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(new BigDecimal("100")) > 0) {
                throw new CouponException(ErrorCode.COUPON_PERCENTAGE_INVALID,
                    "할인율은 0보다 크고 100 이하여야 합니다. 입력값: " + discountValue);
            }
        }

        // 정액 할인일 경우 할인 금액 검증
        if (discountType == DiscountType.FIXED) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(ErrorCode.COUPON_FIXED_AMOUNT_INVALID,
                    "할인 금액은 0보다 커야 합니다. 입력값: " + discountValue);
            }
        }

        // 최소 주문 금액 검증
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CouponException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_INVALID,
                "최소 주문 금액은 0 이상이어야 합니다. 입력값: " + minOrderAmount);
        }

        // 최대 할인 금액 검증 (정률 할인일 때만)
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException(ErrorCode.COUPON_MAX_DISCOUNT_AMOUNT_INVALID,
                "최대 할인 금액은 0보다 커야 합니다. 입력값: " + maxDiscountAmount);
        }

        return new Coupon(
            name,
            code,
            discountType,
            discountValue,
            maxDiscountAmount,
            minOrderAmount,
            totalQuantity,
            0,                      // issuedQuantity (초기값 0)
            perUserLimit,
            startDate,
            endDate,
            true,                   // isActive (초기 상태는 활성)
            null,                    // createdAt
            null                     // updatedAt
        );
    }

    // ===== Validation 메서드 =====

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new CouponException(ErrorCode.COUPON_NAME_REQUIRED);
        }
    }

    private static void validateDiscountType(DiscountType discountType) {
        if (discountType == null) {
            throw new CouponException(ErrorCode.COUPON_DISCOUNT_TYPE_REQUIRED);
        }
    }

    private static void validateDiscountValue(BigDecimal discountValue) {
        if (discountValue == null) {
            throw new CouponException(ErrorCode.COUPON_DISCOUNT_VALUE_REQUIRED);
        }
        if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException(ErrorCode.COUPON_DISCOUNT_VALUE_INVALID);
        }
    }

    private static void validateTotalQuantity(Integer totalQuantity) {
        if (totalQuantity == null || totalQuantity <= 0) {
            throw new CouponException(ErrorCode.COUPON_TOTAL_QUANTITY_INVALID);
        }
    }

    private static void validatePerUserLimit(Integer perUserLimit) {
        if (perUserLimit == null || perUserLimit <= 0) {
            throw new CouponException(ErrorCode.COUPON_PER_USER_LIMIT_INVALID);
        }
    }

    private static void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new CouponException(ErrorCode.COUPON_DATE_REQUIRED);
        }
        if (startDate.isAfter(endDate)) {
            throw new CouponException(ErrorCode.COUPON_INVALID_DATE_RANGE,
                "시작일은 종료일보다 이전이어야 합니다. 시작일: " + startDate + ", 종료일: " + endDate);
        }
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 쿠폰 활성화
     */
    public void activate() {
        if (this.isActive) {
            throw new CouponException(ErrorCode.COUPON_ALREADY_ACTIVE);
        }
        this.isActive = true;
    }

    /**
     * 쿠폰 비활성화
     */
    public void deactivate() {
        if (!this.isActive) {
            throw new CouponException(ErrorCode.COUPON_ALREADY_INACTIVE);
        }
        this.isActive = false;
    }

    /**
     * 쿠폰명 수정
     */
    public void updateName(String name) {
        validateName(name);
        this.name = name;
    }

    /**
     * 쿠폰 코드 수정
     */
    public void updateCode(String code) {
        this.code = code;
    }

    /**
     * 할인 정보 수정
     */
    public void updateDiscountInfo(DiscountType discountType, BigDecimal discountValue) {
        validateDiscountType(discountType);
        validateDiscountValue(discountValue);

        // 정률 할인일 경우 할인율 범위 검증 (0 ~ 100)
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(new BigDecimal("100")) > 0) {
                throw new CouponException(ErrorCode.COUPON_PERCENTAGE_INVALID,
                        "할인율은 0보다 크고 100 이하여야 합니다. 입력값: " + discountValue);
            }
        }

        // 정액 할인일 경우 할인 금액 검증
        if (discountType == DiscountType.FIXED) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(ErrorCode.COUPON_FIXED_AMOUNT_INVALID,
                        "할인 금액은 0보다 커야 합니다. 입력값: " + discountValue);
            }
        }

        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    public void updateDiscountInfo(DiscountType discountType, BigDecimal discountValue, BigDecimal maxDiscountAmount) {
        validateDiscountType(discountType);
        validateDiscountValue(discountValue);

        // 정률 할인일 경우 할인율 범위 검증 (0 ~ 100)
        if (discountType == DiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0 || discountValue.compareTo(new BigDecimal("100")) > 0) {
                throw new CouponException(ErrorCode.COUPON_PERCENTAGE_INVALID,
                    "할인율은 0보다 크고 100 이하여야 합니다. 입력값: " + discountValue);
            }
        }

        // 정액 할인일 경우 할인 금액 검증
        if (discountType == DiscountType.FIXED) {
            if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new CouponException(ErrorCode.COUPON_FIXED_AMOUNT_INVALID,
                    "할인 금액은 0보다 커야 합니다. 입력값: " + discountValue);
            }
        }

        // 최대 할인 금액 검증
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CouponException(ErrorCode.COUPON_MAX_DISCOUNT_AMOUNT_INVALID,
                "최대 할인 금액은 0보다 커야 합니다. 입력값: " + maxDiscountAmount);
        }

        this.discountType = discountType;
        this.discountValue = discountValue;
        this.maxDiscountAmount = maxDiscountAmount;
    }

    /**
     * 최소 주문 금액 수정
     */
    public void updateMinOrderAmount(BigDecimal minOrderAmount) {
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CouponException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_INVALID,
                "최소 주문 금액은 0 이상이어야 합니다. 입력값: " + minOrderAmount);
        }
        this.minOrderAmount = minOrderAmount;
    }

    /**
     * 총 수량 수정
     */
    public void updateTotalQuantity(int totalQuantity) {
        validateTotalQuantity(totalQuantity);

        // 이미 발급된 수량보다 작게 수정할 수 없음
        if (totalQuantity < this.issuedQuantity) {
            throw new CouponException(ErrorCode.COUPON_TOTAL_QUANTITY_INVALID,
                "총 수량은 이미 발급된 수량(" + this.issuedQuantity + ")보다 작을 수 없습니다. 입력값: " + totalQuantity);
        }

        this.totalQuantity = totalQuantity;
    }

    /**
     * 사용자당 제한 수정
     */
    public void updatePerUserLimit(int perUserLimit) {
        validatePerUserLimit(perUserLimit);
        this.perUserLimit = perUserLimit;
    }

    /**
     * 사용 기간 수정
     */
    public void updateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        validateDateRange(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * 현재 시점 기준 쿠폰 유효성 검사 (편의 메서드)
     */
    public boolean isAvailableNow() {
        return isAvailableNow(LocalDateTime.now());
    }

    /**
     * 특정 시점 기준 쿠폰 유효성 검사
     * @param now 검사 기준 시점
     * @return 쿠폰 사용 가능 여부
     */
    public boolean isAvailableNow(LocalDateTime now) {
        if (!this.isActive) {
            return false;
        }

        if (this.startDate != null && this.startDate.isAfter(now)) {
            return false;  // 아직 시작 안 됨
        }

        if (this.endDate != null && this.endDate.isBefore(now)) {
            return false;  // 이미 만료됨
        }

        return true;
    }

    /**
     * 쿠폰 발급 시 수량 증가
     * issuedQuantity는 한번 증가하면 절대 감소하지 않음 (영구적 기록)
     */
    public void increaseIssuedQuantity() {
        if (!hasRemainingQuantity()) {
            throw new CouponException(ErrorCode.COUPON_ALL_ISSUED);
        }
        this.issuedQuantity++;
    }

    /**
     * 발급 가능 여부 (수량 기준)
     */
    public boolean hasRemainingQuantity() {
        return this.issuedQuantity < this.totalQuantity;
    }

    /**
     * 현재 시점 기준 쿠폰 유효성 검증 (편의 메서드)
     */
    public void validateAvailability() {
        validateAvailability(LocalDateTime.now());
    }

    /**
     * 특정 시점 기준 쿠폰 유효성 검증 (예외 발생)
     * @param now 검사 기준 시점
     * @throws CouponException 쿠폰 사용 불가 시
     */
    public void validateAvailability(LocalDateTime now) {
        if (!this.isActive) {
            throw new CouponException(ErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (this.startDate != null && this.startDate.isAfter(now)) {
            throw new CouponException(ErrorCode.COUPON_NOT_STARTED);
        }

        if (this.endDate != null && this.endDate.isBefore(now)) {
            throw new CouponException(ErrorCode.COUPON_EXPIRED);
        }
    }

    /**
     * 주문 금액에 대한 할인 금액 계산
     * @param orderAmount 주문 금액
     * @return 할인 금액
     */
    public BigDecimal calculateDiscountAmount(BigDecimal orderAmount) {
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 최소 주문 금액 검증
        if (this.minOrderAmount != null && orderAmount.compareTo(this.minOrderAmount) < 0) {
            throw new CouponException(ErrorCode.COUPON_MIN_ORDER_AMOUNT_NOT_MET,
                "최소 주문 금액을 충족하지 못했습니다. 최소 주문 금액: " + this.minOrderAmount + ", 현재 주문 금액: " + orderAmount);
        }

        BigDecimal discountAmount;

        if (this.discountType == DiscountType.PERCENTAGE) {
            // 정률 할인: 주문 금액 * (할인율 / 100)
            discountAmount = orderAmount.multiply(this.discountValue).divide(new BigDecimal("100"), 0, BigDecimal.ROUND_DOWN);

            // 최대 할인 금액 제한 적용
            if (this.maxDiscountAmount != null && discountAmount.compareTo(this.maxDiscountAmount) > 0) {
                discountAmount = this.maxDiscountAmount;
            }
        } else if (this.discountType == DiscountType.FIXED) {
            // 정액 할인: 고정 금액
            discountAmount = this.discountValue;

            // 주문 금액보다 할인 금액이 클 수 없음
            if (discountAmount.compareTo(orderAmount) > 0) {
                discountAmount = orderAmount;
            }
        } else {
            throw new CouponException(ErrorCode.COUPON_INVALID_DISCOUNT_TYPE,
                "지원하지 않는 할인 타입입니다: " + this.discountType);
        }

        return discountAmount;
    }
}
