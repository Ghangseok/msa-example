package com.example.msa.order.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 목록의 쪽 지정이다. {@code page}는 0부터, {@code size}는 1 이상 100 이하이고 없으면 20이다. 범위 밖의 값은 틀린 값이다.
 * 20과 100은 환경마다 다른 값이 아니라서 설정값이 아니라 이 클래스의 상수다. Spring을 모르는 순수 Java다.
 */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    /**
     * 글자로 받은 {@code page}와 {@code size}를 검사해서 만든다. 없으면(null) 기본값을 쓴다.
     * 틀린 값이 있으면 모두 모아 {@link InvalidQueryException}을 던진다.
     */
    public static PageRequest of(String pageText, String sizeText) {
        List<InvalidQueryException.Violation> violations = new ArrayList<>();
        int page = 0;
        int size = DEFAULT_SIZE;
        if (pageText != null) {
            Integer parsed = parseInt(pageText);
            if (parsed == null || parsed < 0) {
                violations.add(new InvalidQueryException.Violation("page", "page는 0 이상의 정수여야 한다"));
            } else {
                page = parsed;
            }
        }
        if (sizeText != null) {
            Integer parsed = parseInt(sizeText);
            if (parsed == null || parsed < 1 || parsed > MAX_SIZE) {
                violations.add(new InvalidQueryException.Violation("size", "size는 1 이상 " + MAX_SIZE + " 이하의 정수여야 한다"));
            } else {
                size = parsed;
            }
        }
        if (!violations.isEmpty()) {
            throw new InvalidQueryException(violations);
        }
        return new PageRequest(page, size);
    }

    /** 건너뛸 줄 수다. 앞 쪽들의 주문 수와 같다. {@code page}가 int 최대값이어도 넘치지 않게 long으로 계산한다. */
    public long offset() {
        return (long) page * size;
    }

    /** 읽을 줄 수다. 한 줄 더 읽어서 다음 쪽이 있는지 안다. */
    public int fetchSize() {
        return size + 1;
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
