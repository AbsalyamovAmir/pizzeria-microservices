package ru.cleancode.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.cleancode.orderservice.domain.OrderStatus;
import ru.cleancode.orderservice.dtos.requests.OrderStatusUpdateRequest;
import ru.cleancode.orderservice.services.OrderService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {
    private final OrderService orderService;

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    public void handlePaymentEvent(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        log.info("Received payment event for order: {}", orderId);
        try {
            UUID uuidOrderId = UUID.fromString(orderId);
            switch (event.status()) {
                case "SUCCESS":
                    OrderStatusUpdateRequest processingUpdateRequest = new OrderStatusUpdateRequest(OrderStatus.PROCESSING);
                    orderService.updateOrderStatus(uuidOrderId, processingUpdateRequest);
                    break;
                case "FAILED":
                    orderService.cancelOrder(uuidOrderId, "Payment failed: " + event.reason());
                    break;
                case "REFUNDED":
                    log.info("Payment refund processed for order: {}", orderId);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing payment event for order: {}", orderId, e);
        }
    }

    @KafkaListener(topics = "kitchen-events", groupId = "order-service-group")
    public void handleKitchenEvent(
            @Payload KitchenEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        log.info("Received kitchen event for order: {}", orderId);
        try {
            UUID uuidOrderId = UUID.fromString(orderId);
            switch (event.status()) {
                case "INGREDIENTS_RESERVED":
                    break;
                case "COOKING_STARTED":
                    OrderStatusUpdateRequest cookingUpdateRequest = new OrderStatusUpdateRequest(OrderStatus.COOKING);
                    orderService.updateOrderStatus(uuidOrderId, cookingUpdateRequest);
                    break;
                case "COOKING_COMPLETED":
                    OrderStatusUpdateRequest deliveryStatusUpdateRequest = new OrderStatusUpdateRequest(OrderStatus.READY_FOR_DELIVERY);
                    orderService.updateOrderStatus(uuidOrderId, deliveryStatusUpdateRequest);
                    break;
                case "COOKING_FAILED":
                    orderService.cancelOrder(uuidOrderId, "Cooking failed: " + event.reason());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing kitchen event for order: {}", orderId, e);
        }
    }

    @KafkaListener(topics = "delivery-events", groupId = "order-service-group")
    public void handleDeliveryEvent(
            @Payload DeliveryEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String orderId) {
        log.info("Received delivery event for order: {}", orderId);
        try {
            UUID uuidOrderId = UUID.fromString(orderId);
            OrderStatusUpdateRequest readyStatusUpdateRequest = new OrderStatusUpdateRequest(OrderStatus.READY_FOR_DELIVERY);
            switch (event.status()) {
                case "DELIVERY_STARTED":
                    orderService.updateOrderStatus(uuidOrderId, readyStatusUpdateRequest);
                    break;
                case "DELIVERY_COMPLETED":
                    orderService.updateOrderStatus(uuidOrderId, readyStatusUpdateRequest);
                    break;
                case "DELIVERY_FAILED":
                    orderService.cancelOrder(uuidOrderId, "Delivery failed: " + event.reason());
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing delivery event for order: {}", orderId, e);
        }
    }

    public record PaymentEvent(String status, String reason) {
    }

    public record KitchenEvent(String status, String reason) {
    }

    public record DeliveryEvent(String status, String reason) {
    }
}
