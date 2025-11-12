package io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository;

import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    // 유저 Id와 CouponId로 쿠폰 조회
    Optional<UserCoupon> findByUser_IdAndCoupon_Id(Long userId, Long couponId);

    // 유저 쿠폰 목록 조회
    List<UserCoupon> findByUser_Id(Long userId);

    // 상태별 유저 쿠폰 목록 조회
    List<UserCoupon> findByUser_IdAndStatus(Long userId, UserCouponStatus status);
}
