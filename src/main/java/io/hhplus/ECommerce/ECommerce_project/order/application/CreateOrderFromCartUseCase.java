package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.cart.domain.entity.Cart;
import io.hhplus.ECommerce.ECommerce_project.cart.domain.repository.CartRepository;
import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromCartCommand;
import io.hhplus.ECommerce.ECommerce_project.order.domain.constants.ShippingPolicy;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.OrderItem;
import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderItemRepository;
import io.hhplus.ECommerce.ECommerce_project.order.domain.repository.OrderRepository;
import io.hhplus.ECommerce.ECommerce_project.order.presentation.response.CreateOrderFromCartResponse;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.PointUsageHistory;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointRepository;
import io.hhplus.ECommerce.ECommerce_project.point.domain.repository.PointUsageHistoryRepository;
import io.hhplus.ECommerce.ECommerce_project.product.domain.entity.Product;
import io.hhplus.ECommerce.ECommerce_project.product.domain.repository.ProductRepository;
import io.hhplus.ECommerce.ECommerce_project.user.domain.entity.User;
import io.hhplus.ECommerce.ECommerce_project.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreateOrderFromCartUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;

    @Transactional
    public CreateOrderFromCartResponse execute(CreateOrderFromCartCommand command) {

        // 1. 사용자 확인
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 장바구니 아이템 조회 (cartItemIds)
        List<Cart> cartList = command.cartItemIds().stream()
                .map(cartId -> {
                    Cart cart = cartRepository.findById(cartId)
                            .orElseThrow(() -> new CartException(ErrorCode.CART_NOT_FOUND));

                    // 유저의 카트인지 확인
                    if (!cart.isSameUser(command.userId())) {
                        throw new CartException(ErrorCode.CART_ACCESS_DENIED);
                    }

                    return cart;
                })
                .toList();

        // 3. 각 장바구니 아이템에 대해 상품 검증 및 재고 처리
        // 3-1. 상품별 주문 수량 집계 (같은 상품이 여러 장바구니 항목에 있을 수 있음)
        Map<Long, Integer> productOrderQuantityMap = new HashMap<>();
        for (Cart cart : cartList) {
            productOrderQuantityMap.merge(
                cart.getProduct().getId(),
                cart.getQuantity(),
                Integer::sum
            );
        }

        // 3-2. 장바구니 아이템 오름차순 정렬 (데드락 방지: productId 오름차순 정렬, 원자적 처리)
        // 발생 가능한 데드락 예시 : A가 1번 상품 -> 2번 상품, B가 2번 상품 -> 1번 상품  이런 경우 서로 상품을 점유한 상태에서 안 놔줘서 안 넘어감
        List<Map.Entry<Long, Integer>> sortedEntries = productOrderQuantityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        Map<Long, Product> productMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            Long productId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            // 상품 조회 및 검증 (비관적 락 적용 - 원자적 처리)
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));

            // 주문 가능 여부 검증 (비활성/재고/최소/최대 주문량 체크)
            product.validateOrder(totalQuantity);

            // 재고 차감 및 판매량 증가
            product.decreaseStock(totalQuantity);
            product.increaseSoldCount(totalQuantity);

            // 주문 금액 계산을 위해 productMap에 저장
            productMap.put(productId, product);
        }

        // 4. 주문 금액 계산
        BigDecimal totalAmount = calculateTotalAmount(cartList, productMap);

        // 5. 배송비 계산 (상수 클래스 사용)
        BigDecimal shippingFee = ShippingPolicy.calculateShippingFee(totalAmount);

        // 6. 쿠폰 처리
        BigDecimal discountAmount = BigDecimal.ZERO;

        Coupon coupon = null;

        if (command.couponId() != null) {
            // 6-1. 사용자 쿠폰 조회
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_Id(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 6-2. 쿠폰 조회 및 검증
            coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 6-3. 쿠폰 유효성 검증 (활성화, 기간 등)
            coupon.validateAvailability();

            // 6-4. 사용자 쿠폰 사용 가능 여부 확인
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 6-5. 할인 금액 계산 (최소 주문 금액 검증 포함)
            discountAmount = coupon.calculateDiscountAmount(totalAmount);

            // 6-6. 쿠폰 사용 처리 (usedCount만 증가)
            userCoupon.use(coupon.getPerUserLimit());
        }

        // 7. 포인트 사용 (있으면) - 임시 저장용 리스트
        BigDecimal pointAmount = BigDecimal.ZERO;
        List<Point> pointsToUpdate = new ArrayList<>();  // 사용될 포인트들
        List<BigDecimal> pointUsageAmounts = new ArrayList<>();  // 각 포인트에서 사용할 금액

        if (command.pointAmount() != null
                && command.pointAmount().compareTo(BigDecimal.ZERO) > 0) {

            // 사용 가능한 포인트 조회
            List<Point> availablePoints = pointRepository.findAvailablePointsByUserId(command.userId());

            // 사용 가능한 포인트 합계 계산 (남은 금액 기준)
            BigDecimal totalAvailablePoint = availablePoints.stream()
                    .map(Point::getRemainingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 포인트 잔액 검증
            if (totalAvailablePoint.compareTo(command.pointAmount()) < 0) {
                throw new PointException(ErrorCode.POINT_INSUFFICIENT_POINT);
            }

            // 포인트 사용 처리 (선입선출)
            BigDecimal remainingPointToUse = command.pointAmount();
            for (Point point : availablePoints) {
                if (remainingPointToUse.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                // 해당 포인트에서 사용할 수 있는 금액 계산
                BigDecimal availableAmount = point.getRemainingAmount();
                BigDecimal pointToUse = availableAmount.min(remainingPointToUse);

                // 나중에 사용 이력 생성을 위해 임시 저장
                pointsToUpdate.add(point);
                pointUsageAmounts.add(pointToUse);

                remainingPointToUse = remainingPointToUse.subtract(pointToUse);
            }

            pointAmount = command.pointAmount();
        }

        // 8. Order 생성 (최종 금액은 Order에서 create할때 계산)
        Orders order = Orders.createOrder(
                user,
                coupon,
                totalAmount,            // 상품 총액
                shippingFee,            // 배송비
                discountAmount,         // 쿠폰 할인 금액
                pointAmount             // 포인트 사용 금액
        );

        // 9. 저장
        Orders savedOrder = orderRepository.save(order);

        // 10. 포인트 사용 이력 저장 (Order ID가 필요하므로 주문 생성 후 처리)
        if (!pointUsageAmounts.isEmpty()) {
            for (int i = 0; i < pointUsageAmounts.size(); i++) {
                BigDecimal usageAmount = pointUsageAmounts.get(i);

                // 10-1. 기존 CHARGE/REFUND 포인트는 부분 사용 처리
                Point originalPoint = pointsToUpdate.get(i);
                originalPoint.usePartially(usageAmount);

                // 10-2. PointUsageHistory 생성 (주문과 포인트 연결 추적용)
                PointUsageHistory history = PointUsageHistory.create(
                    originalPoint,
                    savedOrder,
                    usageAmount
                );
                // 10-3. 포인트 사용 내역 저장
                pointUsageHistoryRepository.save(history);
            }

            if (pointAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 10-4. User의 포인트 잔액 차감
                user.usePoint(pointAmount);
            }
        }

        // 11. OrderItem 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProduct().getId());

            OrderItem orderItem = OrderItem.createOrderItem(
                    savedOrder,
                    product,
                    product.getName(),
                    cart.getQuantity(),
                    product.getPrice()
            );
            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);
        }

        // 12. 장바구니 삭제 (물리 삭제)
        for (Cart cart : cartList) {
            cartRepository.deleteById((cart.getId()));
        }

        // 13. 주문 등록 완료 응답 반환 (결제는 별도 API로 처리)
        return CreateOrderFromCartResponse.from(savedOrder, orderItems);
    }

    // 주문 금액 계산 헬퍼 메서드
    private BigDecimal calculateTotalAmount(List<Cart> cartList, Map<Long, Product> productMap) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProduct().getId());

            BigDecimal itemTotalAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(itemTotalAmount);
        }

        return totalAmount;
    }
}
