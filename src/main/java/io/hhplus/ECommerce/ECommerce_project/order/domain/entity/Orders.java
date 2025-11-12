package io.hhplus.ECommerce.ECommerce_project.order.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderStatus;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Orders extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = true)
    private Coupon coupon;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "point_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal pointAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 주문 생성
     */
    public static Orders createOrder(
        User user,
        Coupon coupon,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        BigDecimal discountAmount,
        BigDecimal pointAmount
    ) {
        validateUser(user);
        validateAmount(totalAmount, "총 상품 금액");
        validateAmount(shippingFee, "배송비");

        // 할인 금액 검증 (null 가능)
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderException(ErrorCode.ORDER_DISCOUNT_AMOUNT_INVALID);
        }

        // 포인트 금액 검증 (null 가능)
        if (pointAmount != null && pointAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderException(ErrorCode.ORDER_POINT_AMOUNT_INVALID);
        }

        // 최종 금액 계산
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal point = pointAmount != null ? pointAmount : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.add(shippingFee).subtract(discount).subtract(point);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderException(ErrorCode.ORDER_FINAL_AMOUNT_NEGATIVE);
        }

        LocalDateTime now = LocalDateTime.now();

        return new Orders(
            user,
            coupon,
            totalAmount,
            discount,
            finalAmount,
            shippingFee,
            OrderStatus.PENDING,  // 결제 대기 상태 (PENDING → PAID → COMPLETED)
            point,
            null,   // createdAt
            null,   // updatedAt
            null,  // paidAt
            null   // canceledAt
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 결제 완료 처리 (PENDING → PAID)
     */
    public void paid() {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_PAYMENT,
                "결제 대기 중인 주문만 결제할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderStatus.PAID;
        this.paidAt = now;
    }

    /**
     * 주문 취소 처리 (결제 전, 또는 결제 실패 후)
     * PENDING → CANCELED
     * PAYMENT_FAILED → CANCELED
     */
    public void cancel() {
        if (this.status != OrderStatus.PENDING && this.status != OrderStatus.PAYMENT_FAILED) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_CANCEL,
                "결제 대기 중이거나 결제 실패 상태의 주문만 취소할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderStatus.CANCELED;
        this.canceledAt = now;
    }

    /**
     * 결제 취소 처리 -> 주문 취소 (결제 후)
     */
    public void cancelAfterPaid() {
        if (this.status != OrderStatus.PAID) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_CANCEL,
                "결제 완료된 주문만 결제 취소할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderStatus.CANCELED;
        this.canceledAt = now;
    }

    /**
     * 주문 완료 처리
     */
    public void complete() {
        if (this.status != OrderStatus.PAID) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_COMPLETE,
                "결제 완료된 주문만 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderStatus.COMPLETED;
    }

    /**
     * 결제 실패 처리 (PENDING → PAYMENT_FAILED)
     */
    public void paymentFailed() {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderException(ErrorCode.ORDER_INVALID_STATUS_FOR_PAYMENT,
                "결제 대기 중인 주문만 결제 실패 처리할 수 있습니다. 현재 상태: " + this.status);
        }

        this.status = OrderStatus.PAYMENT_FAILED;
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 결제 완료 여부
     */
    public boolean isPaid() {
        return this.status == OrderStatus.PAID;
    }

    /**
     * 주문 취소 여부
     */
    public boolean isCanceled() {
        return this.status == OrderStatus.CANCELED;
    }

    /**
     * 주문 완료 여부
     */
    public boolean isCompleted() {
        return this.status == OrderStatus.COMPLETED;
    }

    /**
     * 대기 중 여부
     */
    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    /**
     * 결제 실패 여부
     */
    public boolean isPaymentFailed() {
        return this.status == OrderStatus.PAYMENT_FAILED;
    }

    /**
     * 취소 가능 여부 (PENDING 또는 PAYMENT_FAILED 상태만)
     */
    public boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.PAYMENT_FAILED;
    }

    /**
     * 결제 취소 가능 여부 (PAID 상태만)
     */
    public boolean canCancelAfterPaid() {
        return this.status == OrderStatus.PAID;
    }

    /**
     * 무료 배송 여부
     */
    public boolean isFreeShipping() {
        return this.shippingFee.compareTo(BigDecimal.ZERO) == 0;
    }

    // ===== Validation 메서드 =====

    private static void validateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new OrderException(ErrorCode.ORDER_USER_ID_REQUIRED);
        }
    }

    private static void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new OrderException(ErrorCode.ORDER_AMOUNT_REQUIRED, fieldName + "은(는) 필수입니다.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderException(ErrorCode.ORDER_AMOUNT_INVALID, fieldName + "은(는) 0 이상이어야 합니다.");
        }
    }
}
