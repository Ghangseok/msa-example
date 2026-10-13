package com.example.msa.order.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 관리자 조회의 조건이다. 상태와 재고 해제 결과를 각각 생략할 수 있고, 생략하면 null(조건 없음)이다.
 * 이름은 대소문자를 구분한다. 없는 이름이나 빈 글자는 틀린 값이다. Spring을 모르는 순수 Java다.
 */
public record OrderSearch(OrderStatus status, ReleaseStatus releaseStatus) {

    /**
     * 글자로 받은 {@code status}와 {@code releaseStatus}를 검사해서 만든다. 없으면(null) 조건 없음이다.
     * 틀린 값이 있으면 모두 모아 {@link InvalidQueryException}을 던진다.
     */
    public static OrderSearch of(String statusText, String releaseStatusText) {
        List<InvalidQueryException.Violation> violations = new ArrayList<>();
        OrderStatus status = null;
        ReleaseStatus releaseStatus = null;
        if (statusText != null) {
            status = find(OrderStatus.values(), statusText);
            if (status == null) {
                violations.add(invalid("status", OrderStatus.values()));
            }
        }
        if (releaseStatusText != null) {
            releaseStatus = find(ReleaseStatus.values(), releaseStatusText);
            if (releaseStatus == null) {
                violations.add(invalid("releaseStatus", ReleaseStatus.values()));
            }
        }
        if (!violations.isEmpty()) {
            throw new InvalidQueryException(violations);
        }
        return new OrderSearch(status, releaseStatus);
    }

    private static InvalidQueryException.Violation invalid(String field, Enum<?>[] values) {
        String names = Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
        return new InvalidQueryException.Violation(field, field + "는 " + names + " 가운데 하나여야 한다");
    }

    private static <E extends Enum<E>> E find(E[] values, String name) {
        for (E value : values) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
