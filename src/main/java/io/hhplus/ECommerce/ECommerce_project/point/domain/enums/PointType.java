package io.hhplus.ECommerce.ECommerce_project.point.domain.enums;

public enum PointType {

    CHARGE("충전"),
    REFUND("환불");

    private final String description;

    PointType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
