package io.hhplus.ECommerce.ECommerce_project.coupon.infrastructure;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.entity.UserCoupon;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.enums.UserCouponStatus;
import io.hhplus.ECommerce.ECommerce_project.coupon.domain.repository.UserCouponMemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class UserCouponMemoryRepositoryImpl implements UserCouponMemoryRepository {
    private final Map<Long, UserCoupon> userCouponMap = new ConcurrentHashMap<>();
    private final SnowflakeIdGenerator idGenerator;

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        // ID가 없으면 Snowflake ID 생성
        if (userCoupon.getId() == null) {
            userCoupon.setId(idGenerator.nextId());
        }
        userCouponMap.put(userCoupon.getId(), userCoupon);
        return userCoupon;
    }

    @Override
    public Optional<UserCoupon> findById(Long id) {
        return Optional.ofNullable(userCouponMap.get(id));
    }

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponId(Long userId, Long couponId) {
        return userCouponMap.values().stream()
                .filter(userCoupon -> Objects.equals(userCoupon.getUser().getId(), userId)
                        && Objects.equals(userCoupon.getCoupon().getId(), couponId))
                .findFirst();
    }

    @Override
    public List<UserCoupon> findAll() {
        return new ArrayList<>(userCouponMap.values());
    }

    @Override
    public List<UserCoupon> findByUserId(Long userId) {
        return userCouponMap.values().stream()
                .filter(userCoupon -> Objects.equals(userCoupon.getUser().getId(), userId))
                .toList();
    }

    @Override
    public List<UserCoupon> findByUserIdAndStatus(Long userId, UserCouponStatus status) {
        return userCouponMap.values().stream()
                .filter(userCoupon -> Objects.equals(userCoupon.getUser().getId(), userId))
                .filter(userCoupon -> userCoupon.getStatus() == status)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        userCouponMap.remove(id);
    }
}
