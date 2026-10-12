package com.example.msa.order.controller;

import com.example.msa.order.domain.PageRequest;
import com.example.msa.order.dto.OrderDetailResponse;
import com.example.msa.order.dto.OrderPage;
import com.example.msa.order.exception.ProblemDetailsHandler;
import com.example.msa.order.service.OrderQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 조회 API다. 요청 받기, 서비스 호출, 응답 변환만 한다. 업무 판단은 하지 않는다.
 * 고객 ID는 검증한 JWT의 {@code sub}에서만 얻는다. 쿼리 매개변수나 헤더로 받은 고객 ID는 쓰지 않는다.
 * 기준은 specs/002-view-orders/contracts/order-query-api.yaml이다. 클래스에 경로를 두지 않고 메서드마다 쓴다.
 */
@RestController
public class OrderQueryController {

    private final OrderQueryService queryService;

    public OrderQueryController(OrderQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 로그인한 고객 자기 주문 하나를 조회한다. 다른 고객의 주문, 없는 주문, 숫자가 아닌 주문 번호는 모두 같은 404(ORDER_NOT_FOUND)다.
     * 주문 번호는 글자로 받는다. 숫자 변환은 서비스가 한다.
     */
    @GetMapping("/orders/{orderNo}")
    public OrderDetailResponse getMyOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNo) {
        return queryService
                .findMine(jwt.getSubject(), orderNo)
                .orElseThrow(() ->
                        new ErrorResponseException(HttpStatus.NOT_FOUND, ProblemDetailsHandler.orderNotFound(), null));
    }

    /**
     * 로그인한 고객 자기 주문의 목록 한 쪽을 조회한다. 주문 번호가 큰 순서(최근 주문이 먼저)다. 주문이 없으면 빈 목록이다.
     * {@code page}와 {@code size}는 글자로 받아 {@link PageRequest#of}가 검사한다. 알 수 없는 쿼리 매개변수는 무시한다.
     */
    @GetMapping("/orders")
    public OrderPage<OrderDetailResponse> listMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size) {
        return queryService.listMine(jwt.getSubject(), PageRequest.of(page, size));
    }
}
