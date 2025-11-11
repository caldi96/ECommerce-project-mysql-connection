package io.hhplus.ECommerce.ECommerce_project.point.application.dto;

import io.hhplus.ECommerce.ECommerce_project.point.domain.entity.Point;
import lombok.Getter;

import java.util.List;

@Getter
public class PointPageResult {
    private final List<Point> points;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;

    public PointPageResult(List<Point> points, int page, int size, long totalElements, int totalPages, boolean isFirst, boolean isLast) {
        this.points = points;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
    }
}
