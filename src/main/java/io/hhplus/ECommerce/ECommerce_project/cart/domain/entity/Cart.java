package io.hhplus.ECommerce.ECommerce_project.cart.domain.entity;

import io.hhplus.ECommerce.ECommerce_project.common.entity.BaseEntity;
import io.hhplus.ECommerce.ECommerce_project.common.exception.CartException;
import io.hhplus.ECommerce.ECommerce_project.common.exception.ErrorCode;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Cart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 장바구니 아이템 생성
     */
    public static Cart createCart(
            User user,
            Product product,
            int quantity
    ) {
        validateUserId(user.getId());
        validateProductId(product.getId());
        validateQuantity(quantity);

        return new Cart(
            user,
            product,
            quantity,
            null,   // createdAt
            null    // updatedAt
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 수량을 1씩 증가
     */
    public void increaseQuantity() {
        this.quantity++;
    }

    /**
     * 수량을 특정값만큼 증가
     */
    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            throw new CartException(ErrorCode.CART_INCREASE_AMOUNT_INVALID);
        }
        this.quantity += amount;
    }

    /**
     * 수량을 1씩 감소
     */
    public void decreaseQuantity() {
        if (this.quantity <= 1) {
            throw new CartException(ErrorCode.CART_QUANTITY_CANNOT_BE_LESS_THAN_ONE);
        }
        this.quantity--;
    }

    /**
     * 수량을 특정값만큼 감소
     */
    public void decreaseQuantity(int amount) {
        if (amount <= 0) {
            throw new CartException(ErrorCode.CART_DECREASE_AMOUNT_INVALID);
        }
        if (this.quantity - amount < 1) {
            throw new CartException(ErrorCode.CART_QUANTITY_CANNOT_BE_LESS_THAN_ONE);
        }
        this.quantity -= amount;
    }

    /**
     * 수량 변경
     */
    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 동일한 상품인지 확인
     */
    public boolean isSameProduct(Long productId) {
        return this.product.getId().equals(productId);
    }

    /**
     * 동일한 사용자의 장바구니인지 확인
     */
    public boolean isSameUser(Long userId) {
        return this.user.getId().equals(userId);
    }

    // ===== Validation 메서드 =====

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new CartException(ErrorCode.USER_ID_REQUIRED);
        }
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new CartException(ErrorCode.CART_PRODUCT_ID_REQUIRED);
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new CartException(ErrorCode.CART_QUANTITY_INVALID);
        }
    }
}
