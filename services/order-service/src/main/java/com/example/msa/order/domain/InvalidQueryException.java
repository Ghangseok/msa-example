package com.example.msa.order.domain;

import java.io.Serializable;
import java.util.List;

/**
 * 조회 조건(쿼리 매개변수)이 규칙에 맞지 않을 때 던지는 업무 예외다. 틀린 매개변수 이름과 이유의 목록을 담는다.
 * Spring을 모르는 순수 Java라서, 이 예외를 400 응답으로 바꾸는 일은 exception 패키지가 한다.
 */
public class InvalidQueryException extends RuntimeException {

    /** 규칙을 어긴 곳 하나다. {@code field}는 쿼리 매개변수 이름이다. */
    public record Violation(String field, String message) implements Serializable {}

    private final List<Violation> violations;

    public InvalidQueryException(List<Violation> violations) {
        super("조회 조건이 규칙과 맞지 않는다");
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}
