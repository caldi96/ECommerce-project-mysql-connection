package io.hhplus.ECommerce.ECommerce_project.order.application.dto;

import io.hhplus.ECommerce.ECommerce_project.order.domain.entity.Orders;
import lombok.Getter;

import java.util.List;

@Getter
public class OrdersPageResult {
    private final List<Orders> orders;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;

    public OrdersPageResult(List<Orders> orders, int page, int size, long totalElements, int totalPages, boolean isFirst, boolean isLast) {
        this.orders = orders;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }
}
