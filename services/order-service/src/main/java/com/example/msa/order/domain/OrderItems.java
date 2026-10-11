package com.example.msa.order.domain;

import com.example.msa.order.dto.OrderItemRequest;
import com.example.msa.order.dto.OrderWithItems;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 주문 항목 검사 규칙이다. Spring과 MyBatis를 모르는 순수 Java라서 단위 테스트가 쉽다.
 *
 * <p>규칙은 유스케이스 6절 업무 규칙과 data-model.md 5-1절이다. 주문 항목은 1개 이상 20개 이하이고,
 * 각 수량은 1 이상 99 이하이며, 같은 상품은 한 줄에만 나온다. 상품 ID는 영어 대문자·숫자·하이픈 1~20자다.
 */
public final class OrderItems {

    static final int MIN_ITEMS = 1;
    static final int MAX_ITEMS = 20;
    static final int MIN_QUANTITY = 1;
    static final int MAX_QUANTITY = 99;
    private static final Pattern PRODUCT_ID = Pattern.compile("^[A-Z0-9-]{1,20}$");

    /** 검사에 실패한 곳 하나다. {@code field}는 요청 본문 안의 위치(예: items[0].quantity)다. */
    public record Violation(String field, String message) {}

    private OrderItems() {}

    /** 항목을 검사하고, 규칙을 어긴 곳을 모두 돌려준다. 비어 있으면 통과다. */
    public static List<Violation> validate(List<OrderItemRequest> items) {
        List<Violation> violations = new ArrayList<>();
        if (items == null || items.size() < MIN_ITEMS || items.size() > MAX_ITEMS) {
            violations.add(new Violation("items", "주문 항목은 " + MIN_ITEMS + "개 이상 " + MAX_ITEMS + "개 이하여야 한다"));
        }
        if (items == null) {
            return violations;
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            OrderItemRequest item = items.get(i);
            String at = "items[" + i + "]";
            if (item == null) {
                violations.add(new Violation(at, "항목이 비어 있다"));
                continue;
            }
            if (item.productId() == null
                    || !PRODUCT_ID.matcher(item.productId()).matches()) {
                violations.add(new Violation(at + ".productId", "상품 ID는 영어 대문자, 숫자, 하이픈 1~20자여야 한다"));
            } else if (!seen.add(item.productId())) {
                violations.add(new Violation(at + ".productId", "같은 상품은 한 줄에만 나와야 한다"));
            }
            if (!isValidQuantity(item.quantity())) {
                violations.add(new Violation(
                        at + ".quantity", "수량은 " + MIN_QUANTITY + " 이상 " + MAX_QUANTITY + " 이하의 정수여야 한다"));
            }
        }
        return violations;
    }

    /**
     * 요청한 항목과 이미 저장된 항목이 같은 내용인지 본다. 상품 ID 순으로 정렬해 상품과 수량을 비교하고, 항목 순서는 보지 않는다.
     * 같은 고객이 같은 요청 키로 다시 보냈을 때 같은 요청인지, 키만 같고 내용이 다른 요청인지 가른다.
     * 요청 항목은 {@link #validate}를 통과한 것이어야 한다.
     */
    public static boolean sameContent(List<OrderItemRequest> requested, List<OrderWithItems.StoredItem> stored) {
        List<Line> requestedLines = requested.stream()
                .map(item -> new Line(item.productId(), item.quantityAsInt()))
                .sorted(Comparator.comparing(Line::productId))
                .toList();
        List<Line> storedLines = stored.stream()
                .map(item -> new Line(item.productId(), item.quantity()))
                .sorted(Comparator.comparing(Line::productId))
                .toList();
        return requestedLines.equals(storedLines);
    }

    /** 비교에 쓰는 항목 한 줄이다. */
    private record Line(String productId, int quantity) {}

    /** 수량은 소수가 없는 정수이고 1 이상 99 이하여야 한다. */
    private static boolean isValidQuantity(BigDecimal quantity) {
        return quantity != null
                && quantity.stripTrailingZeros().scale() <= 0
                && quantity.compareTo(BigDecimal.valueOf(MIN_QUANTITY)) >= 0
                && quantity.compareTo(BigDecimal.valueOf(MAX_QUANTITY)) <= 0;
    }
}
