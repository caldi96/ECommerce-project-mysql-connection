package io.hhplus.ECommerce.ECommerce_project.cart.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Cart {

    private Long id;
    // 나중에 JPA 연결 시
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_id")
    // private User user;
    private Long userId;
    // 나중에 JPA 연결 시
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "product_id")
    // private Product product;
    private Long productId;
    private int quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 장바구니 아이템 생성
     */
    public static Cart createCart(Long userId, Long productId, int quantity) {
        validateUserId(userId);
        validateProductId(productId);
        validateQuantity(quantity);

        LocalDateTime now = LocalDateTime.now();
        return new Cart(
            null,  // id는 저장 시 생성
            userId,
            productId,
            quantity,
            now,   // createdAt
            now    // updatedAt
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 수량을 1씩 증가
     */
    public void increaseQuantity() {
        this.quantity++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수량을 특정값만큼 증가
     */
    public void increaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("증가량은 1 이상이어야 합니다.");
        }
        this.quantity += amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수량을 1씩 감소
     */
    public void decreaseQuantity() {
        if (this.quantity <= 1) {
            throw new IllegalStateException("수량은 1 미만이 될 수 없습니다. 삭제하려면 장바구니에서 제거하세요.");
        }
        this.quantity--;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수량을 특정값만큼 감소
     */
    public void decreaseQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("감소량은 1 이상이어야 합니다.");
        }
        if (this.quantity - amount < 1) {
            throw new IllegalStateException("수량은 1 미만이 될 수 없습니다. 삭제하려면 장바구니에서 제거하세요.");
        }
        this.quantity -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 수량 변경
     */
    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
        this.updatedAt = LocalDateTime.now();
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 동일한 상품인지 확인
     */
    public boolean isSameProduct(Long productId) {
        return this.productId.equals(productId);
    }

    /**
     * 동일한 사용자의 장바구니인지 확인
     */
    public boolean isSameUser(Long userId) {
        return this.userId.equals(userId);
    }

    // ===== Validation 메서드 =====

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
    }

    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
    }

    // ===== 테스트를 위한 ID 설정 메서드 (인메모리 DB용) =====
    public void setId(Long id) {
        this.id = id;
    }
}
