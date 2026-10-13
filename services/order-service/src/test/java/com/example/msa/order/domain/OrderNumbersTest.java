package com.example.msa.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 주문 번호 변환을 본다. 숫자가 아닌 주문 번호가 "없음"이 되는 근거다(시나리오 1.3). */
class OrderNumbersTest {

    @Test
    @DisplayName("숫자가 아닌 글자 abc는 변환 결과가 비어 있다")
    void nonNumericTextIsEmpty() {
        assertThat(OrderNumbers.parse("abc")).isEmpty();
    }

    @Test
    @DisplayName("글자 9999는 숫자 9999로 바뀐다")
    void numericTextIsParsed() {
        assertThat(OrderNumbers.parse("9999")).hasValue(9999L);
    }
}
