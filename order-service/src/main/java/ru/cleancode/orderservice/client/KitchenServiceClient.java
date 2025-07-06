package ru.cleancode.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.cleancode.orderservice.dtos.KitchenOrderItemDto;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "kitchen-service", url = "${services.kitchen.url}")
public interface KitchenServiceClient {
    @PostMapping("/api/reservations")
    @CircuitBreaker(name = "kitchenService")
    @Retry(name = "kitchenService")
    void reserveIngredients(@RequestParam UUID orderId, @RequestBody List<KitchenOrderItemDto> items);

    @PostMapping("/api/cooking/{orderId}/start")
    @CircuitBreaker(name = "kitchenService")
    @Retry(name = "kitchenService")
    void startCooking(@PathVariable UUID orderId);

    @PostMapping("/api/cooking/{orderId}/cancel")
    @CircuitBreaker(name = "kitchenService")
    @Retry(name = "kitchenService")
    void cancelCooking(@PathVariable UUID orderId);
}
