package io.hhplus.ECommerce.ECommerce_project.product.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA를 위한 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 정적 팩토리 메서드를 위한 private 생성자
public class Product {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    // 나중에 JPA 연결 시
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "category_id")
    // private Category category;
    private Long categoryId;
    private boolean isActive;
    private boolean isSoldOut;
    private int viewCount;
    private int soldCount;
    private Integer minOrderQuantity;
    private Integer maxOrderQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== 정적 팩토리 메서드 =====

    /**
     * 상품 생성
     */
    public static Product createProduct(
        String name,
        String description,
        BigDecimal price,
        int stock,
        Long categoryId,
        Integer minOrderQuantity,
        Integer maxOrderQuantity
    ) {
        validateName(name);
        validatePrice(price);
        validateStock(stock);

        if (minOrderQuantity != null && minOrderQuantity < 1) {
            throw new IllegalArgumentException("최소 주문량은 1 이상이어야 합니다.");
        }

        if (maxOrderQuantity != null && maxOrderQuantity < 1) {
            throw new IllegalArgumentException("최대 주문량은 1 이상이어야 합니다.");
        }

        if (minOrderQuantity != null && maxOrderQuantity != null && minOrderQuantity > maxOrderQuantity) {
            throw new IllegalArgumentException("최소 주문량은 최대 주문량보다 클 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isSoldOut = stock == 0;

        return new Product(
            null,  // id는 저장 시 생성
            name,
            description,
            price,
            stock,
            categoryId,
            true,  // isActive (초기 상태는 활성)
            isSoldOut,
            0,     // viewCount
            0,     // soldCount
            minOrderQuantity,
            maxOrderQuantity,
            now,   // createdAt
            now    // updatedAt
        );
    }

    // ===== 비즈니스 로직 메서드 =====

    /**
     * 재고 차감 (상품 구매 시)
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감할 수량은 1 이상이어야 합니다.");
        }

        if (this.stock < quantity) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + this.stock + ", 요청 수량: " + quantity);
        }

        this.stock -= quantity;
        this.updatedAt = LocalDateTime.now();

        // 재고가 0이 되면 자동으로 품절 처리
        if (this.stock == 0) {
            this.isSoldOut = true;
        }
    }

    /**
     * 재고 증가 (주문 취소, 반품 시)
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("증가할 수량은 1 이상이어야 합니다.");
        }

        this.stock += quantity;
        this.updatedAt = LocalDateTime.now();

        // 재고가 증가하면 품절 상태 해제
        if (this.stock > 0 && this.isSoldOut) {
            this.isSoldOut = false;
        }
    }

    /**
     * 재고 직접 설정 (재고 관리)
     */
    public void updateStock(int stock) {
        validateStock(stock);

        this.stock = stock;
        this.updatedAt = LocalDateTime.now();

        // 재고에 따라 품절 상태 자동 설정
        this.isSoldOut = (stock == 0);
    }

    /**
     * 판매량 증가
     */
    public void increaseSoldCount(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("증가할 판매량은 1 이상이어야 합니다.");
        }

        this.soldCount += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 판매량 감소 (주문 취소 시)
     */
    public void decreaseSoldCount(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("감소할 판매량은 1 이상이어야 합니다.");
        }

        if (this.soldCount < quantity) {
            throw new IllegalStateException("판매량이 취소량보다 작습니다. 현재 판매량: " + this.soldCount + ", 취소량: " + quantity);
        }

        this.soldCount -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 가격 수정
     */
    public void updatePrice(BigDecimal price) {
        validatePrice(price);

        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품명 수정
     */
    public void updateName(String name) {
        validateName(name);

        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품 설명 수정
     */
    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 카테고리 수정
     */
    public void updateCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 최소 주문량 수정
     */
    public void updateMinOrderQuantity(Integer minOrderQuantity) {
        if (minOrderQuantity != null && minOrderQuantity < 1) {
            throw new IllegalArgumentException("최소 주문량은 1 이상이어야 합니다.");
        }

        if (minOrderQuantity != null && this.maxOrderQuantity != null && minOrderQuantity > this.maxOrderQuantity) {
            throw new IllegalArgumentException("최소 주문량은 최대 주문량보다 클 수 없습니다.");
        }

        this.minOrderQuantity = minOrderQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 최대 주문량 수정
     */
    public void updateMaxOrderQuantity(Integer maxOrderQuantity) {
        if (maxOrderQuantity != null && maxOrderQuantity < 1) {
            throw new IllegalArgumentException("최대 주문량은 1 이상이어야 합니다.");
        }

        if (maxOrderQuantity != null && this.minOrderQuantity != null && maxOrderQuantity < this.minOrderQuantity) {
            throw new IllegalArgumentException("최대 주문량은 최소 주문량보다 작을 수 없습니다.");
        }

        this.maxOrderQuantity = maxOrderQuantity;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품 활성화
     */
    public void activate() {
        if (this.isActive) {
            throw new IllegalStateException("이미 활성화된 상품입니다.");
        }

        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상품 비활성화
     */
    public void deactivate() {
        if (!this.isActive) {
            throw new IllegalStateException("이미 비활성화된 상품입니다.");
        }

        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 품절 처리 (수동)
     */
    public void markAsSoldOut() {
        if (this.isSoldOut) {
            throw new IllegalStateException("이미 품절 상태입니다.");
        }

        this.isSoldOut = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 품절 해제 (수동)
     */
    public void markAsAvailable() {
        if (!this.isSoldOut) {
            throw new IllegalStateException("이미 판매 가능 상태입니다.");
        }

        if (this.stock == 0) {
            throw new IllegalStateException("재고가 0인 상품은 판매 가능 상태로 변경할 수 없습니다.");
        }

        this.isSoldOut = false;
        this.updatedAt = LocalDateTime.now();
    }

    // ===== 상태 확인 메서드 =====

    /**
     * 주문 가능 여부
     */
    public boolean canOrder(int quantity) {
        if (!this.isActive) {
            return false;  // 비활성 상품
        }

        if (this.isSoldOut) {
            return false;  // 품절
        }

        if (this.stock < quantity) {
            return false;  // 재고 부족
        }

        if (this.minOrderQuantity != null && quantity < this.minOrderQuantity) {
            return false;  // 최소 주문량 미달
        }

        if (this.maxOrderQuantity != null && quantity > this.maxOrderQuantity) {
            return false;  // 최대 주문량 초과
        }

        return true;
    }

    /**
     * 재고 있음 여부
     */
    public boolean hasStock() {
        return this.stock > 0;
    }

    /**
     * 활성 상품 여부
     */
    public boolean isActiveProduct() {
        return this.isActive;
    }

    /**
     * 품절 여부
     */
    public boolean isSoldOutProduct() {
        return this.isSoldOut;
    }

    // ===== Validation 메서드 =====

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("가격은 필수입니다.");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다.");
        }
    }

    private static void validateStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("재고는 0 이상이어야 합니다.");
        }
    }

    // ===== 테스트를 위한 ID 설정 메서드 (인메모리 DB용) =====
    public void setId(Long id) {
        this.id = id;
    }
}
