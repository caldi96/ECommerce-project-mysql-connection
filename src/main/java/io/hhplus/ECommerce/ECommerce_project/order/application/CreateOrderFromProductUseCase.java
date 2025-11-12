package io.hhplus.ECommerce.ECommerce_project.order.application;

import io.hhplus.ECommerce.ECommerce_project.common.exception.*;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.Coupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.CouponRepository;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponRepository;
import io.hhplus.ECommerce.ECommerce_project.order.application.command.CreateOrderFromProductCommand;
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
public class CreateOrderFromProductUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointRepository pointRepository;
    private final PointUsageHistoryRepository pointUsageHistoryRepository;

    @Transactional
    public CreateOrderFromCartResponse execute(CreateOrderFromProductCommand command) {
        // 보상 트랜잭션을 위한 컨테이너 클래스
        RollbackContext rollbackContext = new RollbackContext();

        try {
            return executeOrder(command, rollbackContext);
        } catch (Exception e) {
            // 보상 트랜잭션 실행
            compensateTransaction(rollbackContext);
            throw e;
        }
    }

    // 추후 jpa 사용시 보상 트랜잭션 제거
    // 롤백 정보를 담는 컨테이너 클래스
    private static class RollbackContext {
        Map<Long, Integer> productQuantityMap = new HashMap<>();
        UserCoupon userCoupon = null;
        Coupon coupon = null;
        Map<Point, BigDecimal> pointUsageMap = new HashMap<>();
        User user = null; // 포인트 사용한 사용자
        BigDecimal usedPointAmount = BigDecimal.ZERO; // 사용한 총 포인트 금액
    }

    private CreateOrderFromCartResponse executeOrder(
            CreateOrderFromProductCommand command,
            RollbackContext rollbackContext) {

        // 1. 사용자 확인
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 수량 검증
        if (command.quantity() == null || command.quantity() <= 0) {
            throw new OrderException(ErrorCode.PRODUCT_QUANTITY_INVALID);
        }

        // 3. 상품 조회 및 재고 차감 (비관적 락 적용 - 원자적 처리)
        // decreaseStockWithLock은 락 안에서 조회, 검증, 재고 차감, 판매량 증가를 원자적으로 수행
        Product product = productRepository.decreaseStockWithLock(command.productId(), command.quantity());
        if (product == null) {
            throw new ProductException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 롤백을 위한 정보 저장
        rollbackContext.productQuantityMap.put(command.productId(), command.quantity());

        // 5. 주문 금액 계산
        BigDecimal totalAmount = product.getPrice()
                .multiply(BigDecimal.valueOf(command.quantity()));

        // 6. 배송비 계산
        BigDecimal shippingFee = ShippingPolicy.calculateShippingFee(totalAmount);

        // 7. 쿠폰 처리
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (command.couponId() != null) {
            // 7-1. 사용자 쿠폰 조회 (미리 발급받아야 함 - 선착순 쿠폰 발급)
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_Id(command.userId(), command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.USER_COUPON_NOT_FOUND));

            // 7-2. 쿠폰 조회 및 검증
            Coupon coupon = couponRepository.findById(command.couponId())
                    .orElseThrow(() -> new CouponException(ErrorCode.COUPON_NOT_FOUND));

            // 7-3. 쿠폰 유효성 검증 (활성화, 기간 등)
            coupon.validateAvailability();

            // 7-4. 사용자 쿠폰 사용 가능 여부 확인
            userCoupon.validateCanUse(coupon.getPerUserLimit());

            // 7-5. 할인 금액 계산
            discountAmount = coupon.calculateDiscountAmount(totalAmount);

            // 7-6. 쿠폰 사용 처리 (usedCount 증가)
            userCoupon.use(coupon.getPerUserLimit());
            userCouponRepository.save(userCoupon);

            // 롤백을 위한 정보 저장
            rollbackContext.userCoupon = userCoupon;
            rollbackContext.coupon = coupon;
        }

        // 8. 포인트 사용
        BigDecimal pointAmount = BigDecimal.ZERO;
        List<Point> pointsToUpdate = new ArrayList<>();
        List<BigDecimal> pointUsageAmounts = new ArrayList<>();

        if (command.pointAmount() != null
                && command.pointAmount().compareTo(BigDecimal.ZERO) > 0) {

            // 사용 가능한 포인트 조회
            List<Point> availablePoints = pointRepository.findAvailablePointsByUserId(command.userId());

            // 사용 가능한 포인트 합계 계산
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

                BigDecimal availableAmount = point.getRemainingAmount();
                BigDecimal pointToUse = availableAmount.min(remainingPointToUse);

                pointUsageAmounts.add(pointToUse);
                pointsToUpdate.add(point);
                remainingPointToUse = remainingPointToUse.subtract(pointToUse);
            }

            pointAmount = command.pointAmount();
        }

        // 9. Order 생성
        Orders order = Orders.createOrder(
                command.userId(),
                totalAmount,
                shippingFee,
                command.couponId(),
                discountAmount,
                pointAmount
        );

        Orders savedOrder = orderRepository.save(order);

        // 10. 포인트 사용 이력 저장 (Order ID가 필요하므로 주문 생성 후 처리)
        if (!pointUsageAmounts.isEmpty()) {
            for (int i = 0; i < pointUsageAmounts.size(); i++) {
                BigDecimal usageAmount = pointUsageAmounts.get(i);

                // 10-1. USE 타입 포인트 생성 (사용 이력 기록용)
                Point usedPoint = Point.use(
                        command.userId(),
                        usageAmount,
                        "주문 결제"
                );
                pointRepository.save(usedPoint);

                // 10-2. 기존 CHARGE/REFUND 포인트는 부분 사용 처리
                Point originalPoint = pointsToUpdate.get(i);
                originalPoint.usePartially(usageAmount);
                pointRepository.save(originalPoint);

                // 10-3. PointUsageHistory 생성 (주문과 포인트 연결 추적용)
                PointUsageHistory history = PointUsageHistory.create(
                        originalPoint,
                        savedOrder,
                        usageAmount
                );
                pointUsageHistoryRepository.save(history);

                // 10-4. 롤백을 위한 정보 저장
                rollbackContext.pointUsageMap.put(originalPoint, usageAmount);
            }

            // 10-5. User의 포인트 잔액 차감
            user.usePoint(pointAmount);
            userRepository.save(user);

            // 10-6. 롤백을 위한 User 정보 저장
            rollbackContext.user = user;
            rollbackContext.usedPointAmount = pointAmount;
        }

        // 11. OrderItem 생성
        OrderItem orderItem = OrderItem.createOrderItem(
                savedOrder.getId(),
                product.getId(),
                product.getName(),
                command.quantity(),
                product.getPrice()
        );
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);
        List<OrderItem> orderItems = List.of(savedOrderItem);

        // 12. 주문 등록 완료 응답 반환 (결제는 별도 API로 처리)
        return CreateOrderFromCartResponse.from(savedOrder, orderItems);
    }

    /**
     * 보상 트랜잭션 실행 (롤백)
     */
    private void compensateTransaction(RollbackContext rollbackContext) {
        // 1. User 포인트 잔액 복구
        if (rollbackContext.user != null && rollbackContext.usedPointAmount.compareTo(BigDecimal.ZERO) > 0) {
            try {
                rollbackContext.user.refundPoint(rollbackContext.usedPointAmount);
                userRepository.save(rollbackContext.user);
            } catch (Exception e) {
                System.err.println("User 포인트 잔액 롤백 실패: " + e.getMessage());
            }
        }

        // 2. 포인트 복구
        for (Map.Entry<Point, BigDecimal> entry : rollbackContext.pointUsageMap.entrySet()) {
            try {
                Point point = entry.getKey();
                BigDecimal usedAmount = entry.getValue();

                point.restoreUsedAmount(usedAmount);
                pointRepository.save(point);
            } catch (Exception e) {
                System.err.println("포인트 롤백 실패: " + e.getMessage());
            }
        }

        // 3. 쿠폰 복구
        if (rollbackContext.userCoupon != null && rollbackContext.coupon != null) {
            try {
                rollbackContext.userCoupon.cancelUse(rollbackContext.coupon.getPerUserLimit());
                userCouponRepository.save(rollbackContext.userCoupon);
            } catch (Exception e) {
                System.err.println("쿠폰 롤백 실패: " + e.getMessage());
            }
        }

        // 4. 상품 재고 복구
        for (Map.Entry<Long, Integer> entry : rollbackContext.productQuantityMap.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            try {
                // ProductMemoryRepository에서 락 안에서 재고 증가 + 판매량 감소 원자 처리
                productRepository.restoreStockWithLock(productId, quantity);
            } catch (Exception e) {
                System.err.println("상품 재고 롤백 실패: " + e.getMessage());
            }
        }
    }
}