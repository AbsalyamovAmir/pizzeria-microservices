package ru.cleancode.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import ru.cleancode.orderservice.domain.OrderStatus;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.topic.order-events}")
    private String orderEventsTopic;

    @Value("${spring.kafka.topic.order-created}")
    private String orderCreatedTopic;

    public void sendOrderCreatedEvent(UUID orderId, Object orderDetails) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(orderCreatedTopic, orderId.toString(), orderDetails);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent order created event for order: {} to topic: {}", orderId, orderCreatedTopic);
            } else {
                log.error("Failed to send order created event for order: {}", orderId, ex);
            }
        });
    }

    public void sendOrderStatusChangedEvent(UUID orderId, OrderStatus status) {
        OrderStatusEvent event = new OrderStatusEvent(orderId, status.toString(), System.currentTimeMillis());
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(orderEventsTopic, orderId.toString(), event);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent status changed event for order: {} to topic: {}", orderId, orderEventsTopic);
            } else {
                log.error("Failed to send status changed event for order: {}", orderId, ex);
            }
        });
    }

    public void sendOrderCancelledEvent(UUID orderId, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(orderId, reason, System.currentTimeMillis());
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(orderEventsTopic, orderId.toString(), event);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent order cancelled event for order: {} to topic: {}", orderId, orderEventsTopic);
            } else {
                log.error("Failed to send order cancelled event for order: {}", orderId, ex);
            }
        });
    }

    public record OrderStatusEvent(UUID orderId, String status, long timestamp) {
    }

    public record OrderCancelledEvent(UUID orderId, String reason, long timestamp) {
    }
}
