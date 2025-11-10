package io.hhplus.ECommerce.ECommerce_project.common.entity;

import io.hhplus.ECommerce.ECommerce_project.common.SnowflakeIdGenerator;
import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BaseEntityListener {

    private static SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    public void setSnowflakeIdGenerator(SnowflakeIdGenerator snowflakeIdGenerator) {
        BaseEntityListener.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    @PrePersist
    public void prePersist(BaseEntity entity) {
        if (entity.getId() == null) {
            entity.setId(snowflakeIdGenerator.nextId());
        }
    }
}