package ru.cleancode.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@FeignClient(name = "delivery-service", url = "${services.delivery.url}")
public interface DeliveryServiceClient {
    @PostMapping("/api/deliveries/{orderId}/schedule")
    @CircuitBreaker(name = "deliveryService")
    @Retry(name = "deliveryService")
    void scheduleDelivery(@PathVariable UUID orderId);

    @PostMapping("/api/deliveries/{orderId}/start")
    @CircuitBreaker(name = "deliveryService")
    @Retry(name = "deliveryService")
    void startDelivery(@PathVariable UUID orderId);
}
