package ru.cleancode.orderservice.services;

import ru.cleancode.orderservice.domain.OrderEvent;
import ru.cleancode.orderservice.dtos.requests.CreateOrderRequest;
import ru.cleancode.orderservice.dtos.OrderDto;
import ru.cleancode.orderservice.dtos.requests.OrderStatusUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderDto createOrder(CreateOrderRequest request);

    OrderDto getOrder(UUID id);

    OrderDto updateOrderStatus(UUID id, OrderStatusUpdateRequest request);

    void cancelOrder(UUID orderId, String reason);

    List<OrderEvent> getOrderEvents(UUID orderId);
}
