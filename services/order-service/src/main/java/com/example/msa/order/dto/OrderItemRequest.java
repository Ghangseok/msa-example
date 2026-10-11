package com.example.msa.order.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * 주문 항목 하나다. contracts/order-api.yaml의 OrderItem과 같다.
 * 상품 ID는 영어 대문자·숫자·하이픈 1~20자, 수량은 1 이상 99 이하의 정수다.
 *
 * <p>수량을 {@link BigDecimal}로 받는 이유가 있다. JSON을 읽는 Jackson은 {@code int} 칸에 들어온 1.5를 1로 잘라서
 * 받아들인다. 그러면 "수량이 정수가 아니다"를 검사할 수 없다. {@code BigDecimal}로 받으면 1.5가 그대로 들어와서,
 * Bean Validation({@code @Digits})과 domain의 {@code OrderItems}가 정수인지 검사할 수 있다.
 * 개수·범위·중복은 domain의 {@code OrderItems}가 검사한다.
 *
 * @param quantity 수량. 소수가 없는 정수만 올바르다
 */
public record OrderItemRequest(
        @NotNull @Pattern(regexp = "^[A-Z0-9-]{1,20}$") String productId,
        @NotNull @Digits(integer = 9, fraction = 0) BigDecimal quantity) {

    /** 정수 수량으로 만든다. */
    public OrderItemRequest(String productId, int quantity) {
        this(productId, BigDecimal.valueOf(quantity));
    }

    /** 검사를 통과한 항목의 수량을 정수로 돌려준다. 검사 전에는 쓰지 않는다. */
    public int quantityAsInt() {
        return quantity.intValueExact();
    }
}
