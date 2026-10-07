package com.example.msa.inventory.exception;

import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 오류 응답을 RFC 9457 Problem Details(application/problem+json)로 바꾼다.
 * 모양은 contracts/inventory-api.yaml의 Problem이다. {@code type}은 {@code urn:msa-example:problem:<코드>}이고,
 * 확장 필드로 {@code code}와 {@code errors}를 둔다. 오류 종류를 코드로 알리므로 코드는 나중에 값을 더해도 계약이 깨지지 않는다.
 *
 * <ul>
 *   <li>400 INVALID_REQUEST: 요청 형식이 틀렸다. 필드별 오류를 errors에 담는다.
 *   <li>409 RESERVATION_CONFLICT: 같은 주문 번호에 다른 내용이 왔다.
 *   <li>500 INTERNAL_ERROR: 교착(ORA-00060) 같은 예상하지 못한 오류. 안쪽 내용은 응답에 담지 않고 로그에 남긴다.
 * </ul>
 *
 * 그 밖의 Spring MVC 표준 오류(없는 경로 404, 지원하지 않는 메서드 405 등)는 Spring의 Problem Details 그대로 둔다.
 */
@RestControllerAdvice
public class ProblemDetailsHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);

    private static final String TYPE_PREFIX = "urn:msa-example:problem:";

    /** errors 항목 하나다. contracts/inventory-api.yaml의 FieldError와 같다. */
    public record FieldProblem(String field, String message) {}

    @ExceptionHandler(ReservationConflictException.class)
    public ProblemDetail handleConflict(ReservationConflictException e) {
        return problem(HttpStatus.CONFLICT, "RESERVATION_CONFLICT", "같은 주문 번호에 다른 내용의 예약 요청이 왔다", e.getMessage());
    }

    /** 예상하지 못한 오류다. 교착이 나도 여기로 온다. 주문 서비스는 500에 재시도하지 않는다. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("예상하지 못한 오류가 났다", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "예상하지 못한 오류가 났다", "잠시 후 다시 시도한다");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldProblem(
                        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName(),
                        error.getDefaultMessage()))
                .toList();
        return handleExceptionInternal(ex, invalidRequest(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    /** 컨트롤러 파라미터(예: 경로의 주문 번호)의 제약을 어겼다. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldProblem> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldProblem(
                                String.valueOf(result.getMethodParameter().getParameterName()),
                                error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(ex, invalidRequest(errors), headers, HttpStatus.BAD_REQUEST, request);
    }

    /** 본문이 JSON이 아니거나, 정수 칸에 정수가 아닌 값이 들어 있다. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = invalidRequest(List.of(new FieldProblem("body", "요청 본문을 읽을 수 없다")));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** 경로의 주문 번호가 숫자가 아니다. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex.getPropertyName() == null ? "path" : ex.getPropertyName();
        ProblemDetail problem = invalidRequest(List.of(new FieldProblem(field, "값의 형식이 맞지 않는다")));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static ProblemDetail invalidRequest(List<FieldProblem> errors) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 잘못됐다", "요청 형식이 계약과 맞지 않는다");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code));
        problem.setTitle(title);
        problem.setProperty("code", code);
        return problem;
    }
}
