package com.example.msa.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * POST /orders의 요청 본문이다. contracts/order-api.yaml의 PlaceOrderRequest와 같다.
 * 고객 ID 필드는 없다. 고객은 토큰의 sub로만 안다.
 */
public record PlaceOrderRequest(@NotNull @Valid List<OrderItemRequest> items) {}
