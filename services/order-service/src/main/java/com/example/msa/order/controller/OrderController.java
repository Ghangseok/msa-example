package com.example.msa.order.controller;

import com.example.msa.order.domain.OrderItems;
import com.example.msa.order.dto.OrderResponse;
import com.example.msa.order.dto.PlaceOrderRequest;
import com.example.msa.order.exception.ProblemDetailsHandler;
import com.example.msa.order.service.OrderService;
import com.example.msa.order.service.OrderService.PlaceResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 고객이 부르는 주문 API다. 요청 받기, 서비스 호출, 응답 변환만 한다. 업무 판단은 하지 않는다.
 * 고객 ID는 검증한 JWT의 {@code sub}에서만 얻는다. 요청 본문이나 다른 헤더로 받은 사용자 정보는 쓰지 않는다.
 * 기준은 specs/001-place-order/contracts/order-api.yaml이다.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    /** UUID 문자열(36자)이다. */
    private static final Pattern UUID_TEXT =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 주문을 요청한다.
     *
     * <ul>
     *   <li>요청 키가 없거나 UUID 형식이 아니면 400(INVALID_IDEMPOTENCY_KEY)이다.
     *   <li>항목이 잘못됐으면 400(INVALID_ORDER_ITEMS)이다. 형식은 Bean Validation이, 개수·범위·중복은
     *       {@link OrderItems#validate}가 검사한다. 어느 경우든 주문을 기록하지 않는다.
     *   <li>새로 기록한 주문은 결과(확정, 거절, 실패)와 관계없이 201이다.
     *   <li>같은 고객이 같은 키로 같은 내용을 다시 보내면 이미 있는 주문의 지금 상태로 200이다.
     *   <li>같은 키에 다른 내용이면 422(IDEMPOTENCY_KEY_CONFLICT)이다.
     * </ul>
     */
    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid PlaceOrderRequest request) {
        if (idempotencyKey == null || !UUID_TEXT.matcher(idempotencyKey).matches()) {
            throw new ErrorResponseException(
                    HttpStatus.BAD_REQUEST, ProblemDetailsHandler.invalidIdempotencyKey(), null);
        }
        List<OrderItems.Violation> violations = OrderItems.validate(request.items());
        if (!violations.isEmpty()) {
            List<ProblemDetailsHandler.FieldProblem> errors = violations.stream()
                    .map(violation -> new ProblemDetailsHandler.FieldProblem(violation.field(), violation.message()))
                    .toList();
            throw new ErrorResponseException(
                    HttpStatus.BAD_REQUEST, ProblemDetailsHandler.invalidOrderItems(errors), null);
        }
        PlaceResult result = orderService.place(jwt.getSubject(), idempotencyKey, request.items());
        return switch (result.outcome()) {
            case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(result.order());
            case EXISTING -> ResponseEntity.ok(result.order());
            case CONFLICT ->
                throw new ErrorResponseException(
                        HttpStatus.UNPROCESSABLE_CONTENT, ProblemDetailsHandler.idempotencyKeyConflict(), null);
        };
    }
}
