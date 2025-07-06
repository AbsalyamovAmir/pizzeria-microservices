package ru.cleancode.orderservice.services.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.cleancode.orderservice.client.DeliveryServiceClient;
import ru.cleancode.orderservice.client.KitchenServiceClient;
import ru.cleancode.orderservice.client.PaymentServiceClient;
import ru.cleancode.orderservice.domain.Order;
import ru.cleancode.orderservice.domain.OrderEvent;
import ru.cleancode.orderservice.domain.OrderStatus;
import ru.cleancode.orderservice.dtos.OrderDto;
import ru.cleancode.orderservice.dtos.requests.CreateOrderRequest;
import ru.cleancode.orderservice.dtos.requests.OrderStatusUpdateRequest;
import ru.cleancode.orderservice.kafka.OrderEventProducer;
import ru.cleancode.orderservice.repositories.OrderEventRepository;
import ru.cleancode.orderservice.repositories.OrderRepository;
import ru.cleancode.orderservice.services.OrderSagaService;
import ru.cleancode.orderservice.services.OrderService;
import ru.cleancode.orderservice.services.OrderTransactionService;
import ru.cleancode.orderservice.utils.OrderMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderMapper orderMapper;
    private final OrderEventProducer orderEventProducer;
    private final KitchenServiceClient kitchenServiceClient;
    private final DeliveryServiceClient deliveryServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderSagaService orderSagaService;
    private final OrderTransactionService orderTransactionService;

    @Transactional
    @Override
    public OrderDto createOrder(CreateOrderRequest request) {
        Order order = orderMapper.toEntity(request);
        order.setItems(request.getItems().stream()
                .map(orderMapper::toItemEntity)
                .peek(item -> item.setOrder(order))
                .collect(Collectors.toSet()));

        Order savedOrder = orderRepository.save(order);
        log.info("Created order with id: {}", savedOrder.getId());

        saveOrderEvent(savedOrder.getId(), "ORDER_CREATED", savedOrder);

        CompletableFuture.runAsync(() -> orderSagaService.processOrderSaga(savedOrder.getId()));

        return orderMapper.toDto(savedOrder);
    }

    @Transactional
    @Override
    public OrderDto updateOrderStatus(UUID id, OrderStatusUpdateRequest request) {
        Order order = validateAndGetOrder(id, request.getStatus());

        orderTransactionService.updateOrderStatus(id, request.getStatus());
        order.setUpdatedAt(LocalDateTime.now());

        saveOrderEvent(id, "ORDER_STATUS_CHANGED", order);
        orderEventProducer.sendOrderStatusChangedEvent(id, request.getStatus());

        handleStatusChangeActions(id, request.getStatus());

        return orderMapper.toDto(order);
    }

    private Order validateAndGetOrder(UUID id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update status of completed/cancelled order");
        }

        return order;
    }

    @Override
    public OrderDto getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return orderMapper.toDto(order);
    }

    private void handleStatusChangeActions(UUID orderId, OrderStatus status) {
        switch (status) {
            case COOKING:
                kitchenServiceClient.startCooking(orderId);
                break;
            case READY_FOR_DELIVERY:
                deliveryServiceClient.scheduleDelivery(orderId);
                break;
            case DELIVERING:
                deliveryServiceClient.startDelivery(orderId);
                break;
            case COMPLETED:
                completeOrder(orderId);
                break;
        }
    }

    private void completeOrder(UUID orderId) {
        log.info("Order {} completed successfully", orderId);
    }

    @Transactional
    @Override
    public void cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }

        orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED);
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        saveOrderEvent(orderId, "ORDER_CANCELLED", Map.of("reason", reason));

        orderEventProducer.sendOrderCancelledEvent(orderId, reason);

        if (order.getStatus().ordinal() >= OrderStatus.PROCESSING.ordinal()) {
            paymentServiceClient.refundPayment(orderId);
        }

        if (order.getStatus().ordinal() >= OrderStatus.COOKING.ordinal()) {
            kitchenServiceClient.cancelCooking(orderId);
        }

        log.info("Order {} cancelled. Reason: {}", orderId, reason);
    }

    private void saveOrderEvent(UUID orderId, String eventType, Object payload) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(orderId);
        event.setEventType(eventType);
        event.setPayload(payload.toString());
        orderEventRepository.save(event);
    }

    @Override
    public List<OrderEvent> getOrderEvents(UUID orderId) {
        return orderEventRepository.findByOrderId(orderId);
    }
}
