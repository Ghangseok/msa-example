package com.example.msa.order.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 주문 서비스(소비자)가 쓰는 재고 API가 저장소 루트 {@code contracts/inventory-api.yaml}을 지키는지 검증한다.
 * WireMock이 받은 요청(주문 서비스가 보낸 요청)은 언제나 검증하고, WireMock이 돌려준 스텁 응답은 계약에 있는 응답일 때 검증한다.
 *
 * <p>계약에 없는 응답 코드(502, 503, 504)와 연결 끊김은 재고 앱이 아니라 네트워크와 인프라의 오류를 흉내 낸 것이다.
 * 그래서 그 스텁의 응답은 검증에서 뺀다. 계약 파일의 폴더는 Gradle이 시스템 속성 {@code msa.contractsDir}로 넘긴다.
 * 이 도우미는 재고 서비스와 공유하지 않는다. 서비스마다 자기 테스트로 계약을 확인한다.
 */
public final class ConsumerContractValidator {

    private static final String CONTRACT_FILE = "inventory-api.yaml";

    /** 재고 앱이 아니라 인프라가 돌려주는 응답 코드다. 계약에 없다. */
    private static final Set<Integer> INFRASTRUCTURE_STATUSES = Set.of(502, 503, 504);

    private final OpenApiInteractionValidator validator;

    public ConsumerContractValidator() {
        String directory = System.getProperty("msa.contractsDir");
        if (directory == null) {
            throw new IllegalStateException("시스템 속성 msa.contractsDir가 없다. Gradle의 test 작업으로 실행한다.");
        }
        Path contract = Path.of(directory, CONTRACT_FILE);
        this.validator = OpenApiInteractionValidator.createFor(contract.toUri().toString())
                .build();
    }

    /** 기록된 요청과 응답을 모두 검증한다. 하나도 없으면 실패한다. 검증할 것이 없는 테스트는 아무것도 증명하지 못하기 때문이다. */
    public void assertAllConform(List<ServeEvent> events) {
        if (events.isEmpty()) {
            throw new AssertionError("검증할 요청이 없다. 주문 서비스가 재고 서비스를 부르지 않았다");
        }
        events.forEach(this::assertConforms);
    }

    /** 요청 하나와 그 응답을 검증한다. 어긋나면 어긋난 곳을 담은 {@link AssertionError}를 던진다. */
    public void assertConforms(ServeEvent event) {
        LoggedRequest logged = event.getRequest();
        String method = logged.getMethod().getName();
        String path = pathOf(logged.getUrl());
        if (!event.getWasMatched()) {
            throw new AssertionError("스텁에 맞지 않은 요청이다: " + method + " " + path);
        }
        Request request = request(method, path, logged);
        ValidationReport report;
        if (skipsResponse(event)) {
            report = validator.validateRequest(request);
        } else {
            report = validator.validate(request, response(event));
        }
        if (report.hasErrors()) {
            String messages = report.getMessages().stream()
                    .map(message -> "  - " + message.getMessage())
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new AssertionError("계약(" + CONTRACT_FILE + ")과 어긋난다: " + method + " " + path + " → "
                    + event.getResponse().getStatus() + System.lineSeparator() + messages);
        }
    }

    private static boolean skipsResponse(ServeEvent event) {
        return event.getResponse().getFault() != null
                || INFRASTRUCTURE_STATUSES.contains(event.getResponse().getStatus());
    }

    private static String pathOf(String url) {
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query);
    }

    private static Request request(String method, String path, LoggedRequest logged) {
        SimpleRequest.Builder builder = switch (method) {
            case "PUT" -> SimpleRequest.Builder.put(path);
            case "DELETE" -> SimpleRequest.Builder.delete(path);
            default -> throw new IllegalArgumentException("재고 API에 없는 메서드다: " + method);
        };
        String body = logged.getBodyAsString();
        if (body != null && !body.isEmpty()) {
            builder.withBody(body);
        }
        ContentTypeHeader contentType = logged.getHeaders().getContentTypeHeader();
        if (contentType != null && contentType.isPresent()) {
            builder.withContentType(contentType.firstValue());
        }
        return builder.build();
    }

    private static Response response(ServeEvent event) {
        SimpleResponse.Builder builder =
                SimpleResponse.Builder.status(event.getResponse().getStatus());
        ContentTypeHeader contentType = event.getResponse().getHeaders().getContentTypeHeader();
        if (contentType != null && contentType.isPresent()) {
            builder.withContentType(contentType.firstValue());
        }
        String body = event.getResponse().getBodyAsString();
        if (body != null && !body.isEmpty()) {
            builder.withBody(body);
        }
        return builder.build();
    }
}
