package ru.cleancode.orderservice.services;

import ru.cleancode.orderservice.domain.OrderStatus;

import java.util.UUID;

public interface OrderTransactionService {
    void updateOrderStatus(UUID orderId, OrderStatus status);

    void cancelOrder(UUID orderId, String reason);
}
