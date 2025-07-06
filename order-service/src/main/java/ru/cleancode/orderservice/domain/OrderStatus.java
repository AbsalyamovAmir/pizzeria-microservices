package ru.cleancode.orderservice.domain;

public enum OrderStatus {
    CREATED,
    PROCESSING,
    COOKING,
    READY_FOR_DELIVERY,
    DELIVERING,
    COMPLETED,
    CANCELLED
}
