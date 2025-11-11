package io.hhplus.ECommerce.ECommerce_project.product.application.command;

import java.math.BigDecimal;

public record CreateProductCommand(
        Long categoryId,
        String name,
        String description,
        BigDecimal price,
        int stock,
        Integer minOrderQuantity,
        Integer maxOrderQuantity
) {}
