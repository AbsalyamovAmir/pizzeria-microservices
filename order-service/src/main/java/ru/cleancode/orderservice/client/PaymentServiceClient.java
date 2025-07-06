package ru.cleancode.orderservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.cleancode.orderservice.dtos.PaymentRequestDto;

import java.util.UUID;

@FeignClient(name = "payment-service", url = "${services.payment.url}")
public interface PaymentServiceClient {
    @PostMapping("/api/payments/process")
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    void processPayment(@RequestBody PaymentRequestDto request);

    @PostMapping("/api/payments/{orderId}/refund")
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    void refundPayment(@PathVariable UUID orderId);
}
