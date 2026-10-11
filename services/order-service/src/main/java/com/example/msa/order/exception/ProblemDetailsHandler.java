package com.example.msa.order.exception;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 오류 응답을 RFC 9457 Problem Details(application/problem+json)로 바꾼다.
 * 모양은 contracts/order-api.yaml의 Problem이다. {@code type}은 {@code urn:msa-example:problem:<코드>}이고,
 * 확장 필드로 {@code code}와 {@code errors}를 둔다. 오류 종류를 코드로 알리므로 코드는 나중에 값을 더해도 계약이 깨지지 않는다.
 *
 * <ul>
 *   <li>400 INVALID_ORDER_ITEMS: 주문 항목이 잘못됐다. 필드별 오류를 errors에 담는다.
 *   <li>400 INVALID_IDEMPOTENCY_KEY: 주문 요청 키가 없거나 UUID 형식이 아니다.
 *   <li>422 IDEMPOTENCY_KEY_CONFLICT: 같은 요청 키에 다른 내용이 왔다.
 * </ul>
 *
 * 401(UNAUTHORIZED)은 필터에서 나므로 {@link ProblemAuthenticationEntryPoint}가 같은 모양으로 만든다.
 * 그 밖의 Spring MVC 표준 오류(없는 경로 404, 지원하지 않는 메서드 405 등)는 Spring의 Problem Details 그대로 둔다.
 */
@RestControllerAdvice
public class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

    static final String TYPE_PREFIX = "urn:msa-example:problem:";

    /** errors 항목 하나다. contracts/order-api.yaml의 FieldError와 같다. */
    public record FieldProblem(String field, String message) {}

    /** 주문 항목이 잘못됐다(400 INVALID_ORDER_ITEMS). 주문을 기록하지 않고 재고 서비스에도 요청하지 않는다. */
    public static ProblemDetail invalidOrderItems(List<FieldProblem> errors) {
        ProblemDetail problem =
                problem(HttpStatus.BAD_REQUEST, "INVALID_ORDER_ITEMS", "주문 항목이 잘못됐다", "주문 항목이 규칙과 맞지 않는다");
        problem.setProperty("errors", errors);
        return problem;
    }

    /** 주문 요청 키가 없거나 UUID 형식이 아니다(400 INVALID_IDEMPOTENCY_KEY). 주문을 기록하지 않는다. */
    public static ProblemDetail invalidIdempotencyKey() {
        return problem(
                HttpStatus.BAD_REQUEST,
                "INVALID_IDEMPOTENCY_KEY",
                "주문 요청 키가 잘못됐다",
                "Idempotency-Key 헤더는 UUID 문자열(36자)이어야 한다");
    }

    /** 같은 요청 키에 다른 내용이 왔다(422 IDEMPOTENCY_KEY_CONFLICT). 새 주문을 만들지 않는다. */
    public static ProblemDetail idempotencyKeyConflict() {
        return problem(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "IDEMPOTENCY_KEY_CONFLICT",
                "같은 요청 키에 다른 내용이 왔다",
                "이 요청 키로 이미 다른 내용의 주문이 있다. 새 주문은 새 요청 키로 보낸다");
    }

    /** 요청 본문의 형식 검사(Bean Validation)에 실패했다. 예: 상품 ID가 비었다. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldProblem(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();
        return handleExceptionInternal(ex, invalidOrderItems(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    /** 본문이 JSON이 아니거나, 정수 칸에 정수가 아닌 값(예: 수량 1.5)이 들어 있다. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = invalidOrderItems(List.of(new FieldProblem("body", "요청 본문을 읽을 수 없다")));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** 계약의 Problem 모양으로 만든다. {@code code}는 오류 종류다. */
    static ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }
}
