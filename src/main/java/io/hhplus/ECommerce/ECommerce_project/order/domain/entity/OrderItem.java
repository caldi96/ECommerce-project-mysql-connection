package io.hhplus.ECommerce.ECommerce_project.order.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.common.exception.OrderException;
import io.hhplus.ECommerce.ECommerce_project.order.domain.enums.OrderItemStatus;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders orders;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "sub_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal subTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderItemStatus status;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 주문 항목 생성
     */
    public static OrderItem createOrderItem(
        Orders orders,
        Product product,
        String productName,
        int quantity,
        BigDecimal unitPrice
    ) {
        validateOrder(orders);
        validateProduct(product);
        validateProductName(productName);
        validateQuantity(quantity);
        validateUnitPrice(unitPrice);

        // subTotal 계산
        BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        return new OrderItem(
            product,
            orders,
            productName,
            quantity,
            unitPrice,
            subTotal,
            OrderItemStatus.ORDER_PENDING,  // 초기 상태
            null,  // confirmedAt
            null,  // canceledAt
            null,  // returnedAt
            null,  // refundedAt
            null,   // createdAt
            null    // updatedAt
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 주문 항목 완료 처리
     */
    public void complete() {
        if (this.status != OrderItemStatus.ORDER_PENDING) {
            throw new OrderException(ErrorCode.ORDER_ITEM_INVALID_STATUS_FOR_COMPLETE,
                "대기 중인 주문 항목만 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderItemStatus.ORDER_COMPLETED;
    }

    /**
     * 주문 항목 취소 (주문 완료 전)
     */
    public void cancel() {
        if (this.status != OrderItemStatus.ORDER_PENDING) {
            throw new OrderException(ErrorCode.ORDER_ITEM_INVALID_STATUS_FOR_CANCEL,
                "대기 중인 주문 항목만 취소할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderItemStatus.ORDER_CANCELED;
        this.canceledAt = now;
    }

    /**
     * 주문 항목 취소 (주문 완료 후)
     */
    public void cancelAfterComplete() {
        if (this.status != OrderItemStatus.ORDER_COMPLETED) {
            throw new OrderException(ErrorCode.ORDER_ITEM_INVALID_STATUS_FOR_CANCEL,
                "완료된 주문 항목만 취소할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderItemStatus.ORDER_CANCELED;
        this.canceledAt = now;
    }

    /**
     * 상품 항목 반품
     */
    public void returnItem() {
        if (this.status != OrderItemStatus.ORDER_COMPLETED && this.status != OrderItemStatus.PURCHASE_CONFIRMED) {
            throw new OrderException(ErrorCode.ORDER_ITEM_INVALID_STATUS_FOR_RETURN,
                "완료 또는 구매 확정된 주문 항목만 반품할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderItemStatus.ORDER_RETURNED;
        this.returnedAt = now;
    }

    /**
     * 상품 항목 환불
     */
    public void refund() {
        if (this.status != OrderItemStatus.ORDER_CANCELED && this.status != OrderItemStatus.ORDER_RETURNED) {
            throw new OrderException(ErrorCode.ORDER_ITEM_INVALID_STATUS_FOR_REFUND,
                "취소 또는 반품된 주문 항목만 환불할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderItemStatus.ORDER_REFUNDED;
        this.refundedAt = now;
    }

    /**
     * 상품 항목 구매 확정
     */
    public void confirmPurchase() {
        if (this.status != OrderItemStatus.ORDER_COMPLETED) {
            throw new OrderException(ErrorCode.ORDER_ITEM_INVALID_STATUS_FOR_CONFIRM,
                "완료된 주문 항목만 구매 확정할 수 있습니다. 현재 상태: " + this.status);
        }

        LocalDateTime now = LocalDateTime.now();
        this.status = OrderItemStatus.PURCHASE_CONFIRMED;
        this.confirmedAt = now;
    }

    /**
     * 주문 항목 총 금액 재계산
     */
    public void recalculateSubTotal() {
        validateUnitPrice(this.unitPrice);
        validateQuantity(this.quantity);

        this.subTotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 대기 중 여부
     */
    public boolean isPending() {
        return this.status == OrderItemStatus.ORDER_PENDING;
    }

    /**
     * 완료 여부
     */
    public boolean isCompleted() {
        return this.status == OrderItemStatus.ORDER_COMPLETED;
    }

    /**
     * 취소 여부
     */
    public boolean isCanceled() {
        return this.status == OrderItemStatus.ORDER_CANCELED;
    }

    /**
     * 반품 여부
     */
    public boolean isReturned() {
        return this.status == OrderItemStatus.ORDER_RETURNED;
    }

    /**
     * 환불 여부
     */
    public boolean isRefunded() {
        return this.status == OrderItemStatus.ORDER_REFUNDED;
    }

    /**
     * 구매 확정 여부
     */
    public boolean isPurchaseConfirmed() {
        return this.status == OrderItemStatus.PURCHASE_CONFIRMED;
    }

    /**
     * 반품 가능 여부
     */
    public boolean canReturn() {
        return this.status == OrderItemStatus.ORDER_COMPLETED
            || this.status == OrderItemStatus.PURCHASE_CONFIRMED;
    }

    /**
     * 환불 가능 여부
     */
    public boolean canRefund() {
        return this.status == OrderItemStatus.ORDER_CANCELED
            || this.status == OrderItemStatus.ORDER_RETURNED;
    }

    // ===== Validation 메서드 =====

    private static void validateOrder(Orders orders) {
        if (orders == null || orders.getId() == null) {
            throw new OrderException(ErrorCode.ORDER_ITEM_ORDER_ID_REQUIRED);
        }
    }

    private static void validateProduct(Product product) {
        if (product == null || product.getId() == null) {
            throw new OrderException(ErrorCode.ORDER_ITEM_PRODUCT_ID_REQUIRED);
        }
    }

    private static void validateProductName(String productName) {
        if (productName == null || productName.trim().isEmpty()) {
            throw new OrderException(ErrorCode.ORDER_ITEM_PRODUCT_NAME_REQUIRED);
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new OrderException(ErrorCode.ORDER_ITEM_QUANTITY_INVALID);
        }
    }

    private static void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new OrderException(ErrorCode.ORDER_ITEM_UNIT_PRICE_REQUIRED);
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderException(ErrorCode.ORDER_ITEM_UNIT_PRICE_INVALID);
        }
    }
}