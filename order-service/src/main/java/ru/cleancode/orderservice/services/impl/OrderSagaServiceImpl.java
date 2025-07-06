package ru.cleancode.orderservice.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import ru.cleancode.orderservice.client.KitchenServiceClient;
import ru.cleancode.orderservice.client.PaymentServiceClient;
import ru.cleancode.orderservice.domain.Order;
import ru.cleancode.orderservice.domain.OrderItem;
import ru.cleancode.orderservice.domain.OrderStatus;
import ru.cleancode.orderservice.dtos.KitchenOrderItemDto;
import ru.cleancode.orderservice.dtos.PaymentRequestDto;
import ru.cleancode.orderservice.repositories.OrderRepository;
import ru.cleancode.orderservice.services.OrderSagaService;
import ru.cleancode.orderservice.services.OrderTransactionService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaServiceImpl implements OrderSagaService {
    private final OrderRepository orderRepository;
    private final KitchenServiceClient kitchenServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderTransactionService orderTransactionService;

    @Retryable(maxAttempts = 3)
    @Override
    public void processOrderSaga(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        try {
            reserveIngredients(order);

            processPayment(order);

            orderTransactionService.updateOrderStatus(orderId, OrderStatus.PROCESSING);

            log.info("Order saga completed successfully for order: {}", orderId);
        } catch (Exception e) {
            log.error("Order saga failed for order: {}", orderId, e);
            orderTransactionService.cancelOrder(orderId, "Saga failed: " + e.getMessage());
            throw e;
        }
    }

    private void reserveIngredients(Order order) {
        kitchenServiceClient.reserveIngredients(order.getId(), order.getItems().stream()
                .map(item -> new KitchenOrderItemDto(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getSpecialRequests()))
                .collect(Collectors.toList()));
    }

    private void processPayment(Order order) {
        paymentServiceClient.processPayment(new PaymentRequestDto(
                order.getId(),
                order.getCustomerId(),
                calculateTotalPrice(order.getItems()),
                "Pizza order #" + order.getId()));
    }

    private BigDecimal calculateTotalPrice(Set<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
