package ru.cleancode.orderservice.dtos.requests;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotEmpty(message = "Delivery address is required")
    private String deliveryAddress;

    @NotEmpty(message = "Order items are required")
    @Valid
    private List<OrderItemRequest> items;
}
