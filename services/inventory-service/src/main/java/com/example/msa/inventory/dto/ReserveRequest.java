package com.example.msa.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PUT /reservations/{orderNo}의 요청 본문이다. contracts/inventory-api.yaml의 ReserveRequest와 같다.
 * 항목은 1개 이상 20개 이하이고, 같은 상품은 한 줄에만 나온다. 이 규칙은 계약에 적은 형식을 서비스가 직접 지키게 하려고 검사한다.
 */
public record ReserveRequest(
        @NotNull @Size(min = 1, max = 20) @Valid List<Item> items) {

    /** 예약할 항목 하나다. 상품 ID는 영어 대문자·숫자·하이픈 1~20자, 수량은 1 이상 99 이하다. */
    public record Item(
            @NotNull @Pattern(regexp = "^[A-Z0-9-]{1,20}$") String productId,
            @Min(1) @Max(99) int quantity) {}

    /** 같은 상품은 한 줄에만 나와야 한다. */
    @AssertTrue(message = "같은 상품은 한 줄에만 나와야 한다")
    public boolean isProductIdsDistinct() {
        if (items == null) {
            return true;
        }
        Set<String> seen = new HashSet<>();
        for (Item item : items) {
            if (item != null && !seen.add(item.productId())) {
                return false;
            }
        }
        return true;
    }
}
