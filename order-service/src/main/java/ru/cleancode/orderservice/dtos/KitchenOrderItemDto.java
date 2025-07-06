package ru.cleancode.orderservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenOrderItemDto {
    private UUID productId;
    private Integer quantity;
    private String specialRequests;
}
