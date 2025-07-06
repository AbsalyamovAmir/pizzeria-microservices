package ru.cleancode.orderservice.services;

import java.util.UUID;

public interface OrderSagaService {
    void processOrderSaga(UUID orderId);
}
