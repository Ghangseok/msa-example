package com.example.msa.order.domain;

import java.util.OptionalLong;

/**
 * 주문 번호 글자를 숫자로 바꾸는 순수 Java 도구다. Spring과 MyBatis를 모른다.
 * 숫자 타입으로 받으면 숫자가 아닌 글자에 Spring이 400을 내는데, 조회는 404("없음")를 요구한다. 그래서 글자로 받아 여기서 바꾼다.
 */
public final class OrderNumbers {

    private OrderNumbers() {}

    /** 숫자면 주문 번호를 돌려주고, 숫자가 아니거나 {@code long}에 담기지 않으면 빈 값을 돌려준다. 예외를 던지지 않는다. */
    public static OptionalLong parse(String text) {
        try {
            return OptionalLong.of(Long.parseLong(text));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }
}
