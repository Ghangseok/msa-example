package com.example.msa.inventory.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * 서비스 통합 테스트가 보낸 요청과 받은 응답이 저장소 루트 {@code contracts/inventory-api.yaml}을 지키는지 검증한다.
 * 이 재고 서비스가 계약을 내주는 쪽(제공자)이다. 계약 파일의 폴더는 Gradle이 시스템 속성 {@code msa.contractsDir}로 넘긴다.
 * 이 도우미는 주문 서비스와 공유하지 않는다. 서비스마다 자기 테스트로 계약을 확인한다.
 */
public final class ProviderContractValidator {

    private static final String CONTRACT_FILE = "inventory-api.yaml";

    private final OpenApiInteractionValidator validator;

    public ProviderContractValidator() {
        String directory = System.getProperty("msa.contractsDir");
        if (directory == null) {
            throw new IllegalStateException("시스템 속성 msa.contractsDir가 없다. Gradle의 test 작업으로 실행한다.");
        }
        Path contract = Path.of(directory, CONTRACT_FILE);
        this.validator = OpenApiInteractionValidator.createFor(contract.toUri().toString())
                .build();
    }

    /**
     * 요청과 응답이 계약에 맞는지 검사한다. 어긋나면 어긋난 곳을 담은 {@link AssertionError}를 던진다.
     *
     * @param method HTTP 메서드(PUT 또는 DELETE)
     * @param path 쿼리 없는 경로. 예: /reservations/1001
     * @param requestBody 요청 본문. 없으면 null
     * @param status 응답 상태 코드
     * @param responseContentType 응답 Content-Type. 없으면 null
     * @param responseBody 응답 본문. 없으면 null
     */
    public void assertConforms(
            String method,
            String path,
            String requestBody,
            int status,
            String responseContentType,
            String responseBody) {
        SimpleResponse.Builder response = SimpleResponse.Builder.status(status);
        if (responseContentType != null) {
            response.withContentType(responseContentType);
        }
        if (responseBody != null) {
            response.withBody(responseBody);
        }
        ValidationReport report = validator.validate(request(method, path, requestBody), response.build());
        if (report.hasErrors()) {
            String messages = report.getMessages().stream()
                    .map(message -> "  - " + message.getMessage())
                    .collect(Collectors.joining(System.lineSeparator()));
            throw new AssertionError("계약(" + CONTRACT_FILE + ")과 어긋난다: " + method + " " + path + " → " + status
                    + System.lineSeparator() + messages);
        }
    }

    private static Request request(String method, String path, String body) {
        SimpleRequest.Builder builder = switch (method) {
            case "PUT" -> SimpleRequest.Builder.put(path);
            case "DELETE" -> SimpleRequest.Builder.delete(path);
            default -> throw new IllegalArgumentException("재고 API에 없는 메서드다: " + method);
        };
        if (body != null) {
            builder.withContentType("application/json").withBody(body);
        }
        return builder.build();
    }
}
