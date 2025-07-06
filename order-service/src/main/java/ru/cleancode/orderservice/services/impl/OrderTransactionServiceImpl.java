package ru.cleancode.orderservice.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cleancode.orderservice.domain.OrderEvent;
import ru.cleancode.orderservice.domain.OrderStatus;
import ru.cleancode.orderservice.kafka.OrderEventProducer;
import ru.cleancode.orderservice.repositories.OrderEventRepository;
import ru.cleancode.orderservice.repositories.OrderRepository;
import ru.cleancode.orderservice.services.OrderTransactionService;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTransactionServiceImpl implements OrderTransactionService {
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    @Override
    public void updateOrderStatus(UUID orderId, OrderStatus status) {
        orderRepository.updateOrderStatus(orderId, status);
    }

    @Transactional
    @Override
    public void cancelOrder(UUID orderId, String reason) {
        orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED);
        saveCancellationEvent(orderId, reason);
        orderEventProducer.sendOrderCancelledEvent(orderId, reason);
    }

    private void saveCancellationEvent(UUID orderId, String reason) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setEventType("ORDER_CANCELLED");
        event.setPayload(Map.of("reason", reason).toString());
        orderEventRepository.save(event);
    }
}
