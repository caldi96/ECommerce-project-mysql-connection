package io.hhplus.ECommerce.ECommerce_project.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 모든 엔티티의 기본 클래스
 * - Snowflake ID 자동 생성
 */
@MappedSuperclass
@EntityListeners(BaseEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    private Long id;
}