package ru.cleancode.orderservice.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.cleancode.orderservice.domain.Order;
import ru.cleancode.orderservice.domain.OrderItem;
import ru.cleancode.orderservice.dtos.requests.CreateOrderRequest;
import ru.cleancode.orderservice.dtos.OrderDto;
import ru.cleancode.orderservice.dtos.requests.OrderItemRequest;

import java.math.BigDecimal;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "totalPrice", source = "items", qualifiedByName = "calculateTotalPrice")
    OrderDto toDto(Order order);

    @Named("calculateTotalPrice")
    default BigDecimal calculateTotalPrice(Set<OrderItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", expression = "java(ru.cleancode.orderservice.domain.OrderStatus.CREATED)")
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toItemEntity(OrderItemRequest request);
}
