package com.example.msa.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.msa.order.client.InventoryCallContext;
import com.example.msa.order.client.InventoryCallFailure;
import com.example.msa.order.client.InventoryClient;
import com.example.msa.order.dto.OrderItemRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * 재고 호출의 시도 하나가 시도당 제한 시간을 지키는지, 연결 시간까지 포함해서 지키는지 본다.
 * 인수 시나리오 테스트가 아니라서 테스트 케이스 번호를 붙이지 않는다.
 *
 * <p>기대값은 비기능 요구사항 문서(docs/requirements/non-functional.md)의 "장애 전파 차단" 목표값
 * ("재시도 포함 전체 30초, 시도당 2.5초, 재시도 최대 5회(즉시, 1, 2, 4, 8초 뒤). 서킷이 열린 뒤에는 1초 안에 실패 응답")의
 * "시도당 2.5초"다. 이 테스트는 시간 값을 1/5로 줄인 테스트 설정을 쓰므로 500ms다 [제안].
 * "3초 뒤에 응답"은 600ms로 줄인다.
 *
 * <p>JDK HttpClient의 요청 제한 시간이 연결 시간까지 포함하는지는 research.md 6절에서 확인하지 못한 것이다. 이 테스트가 확인한다.
 * 응답하지 않는 주소는 RFC 5737이 문서용으로 예약한 192.0.2.1이다 [제안]. 이 PC에서 실험해 보니 연결을 시도하면 약 500ms 뒤
 * 연결 시간 초과로 끝났다. 로컬 서버 소켓의 대기열을 채워 연결을 기다리게 하는 방법은 이 PC(Windows)가 연결을 조용히 버리지 않고
 * 거부해서 쓰지 못했다. 네트워크가 이 주소를 바로 거부하는 환경에서는 시간이 짧게 끝나서 이 테스트가 실패한다. 그때는 환경이 이 확인에
 * 맞지 않는 것이다.
 */
class InventoryClientTimeoutIntegrationTest extends InventoryWireMock {

    private static final Logger log = LoggerFactory.getLogger(InventoryClientTimeoutIntegrationTest.class);

    /** 테스트 설정의 시도당 제한 시간(500ms)보다 조금 작은 값이다. 시간 초과가 제한 시간보다 일찍 나지 않았는지 본다. */
    private static final Duration EARLIEST_TIMEOUT = Duration.ofMillis(450);

    /**
     * 제한 시간 500ms에 처음 호출의 준비 시간을 더한 여유다. 연결 제한 시간과 요청 제한 시간을 더한 1000ms보다 작아서,
     * 두 시간이 더해지면 이 상한을 넘는다.
     */
    private static final Duration LATEST_TIMEOUT = Duration.ofMillis(800);

    /** 연결에 응답하지 않는 주소다. RFC 5737이 문서용으로 예약한 TEST-NET-1이라 라우팅되지 않는다. */
    private static final String UNRESPONSIVE_ADDRESS = "192.0.2.1";

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    @Qualifier("inventoryRestClient")
    private RestClient inventoryRestClient;

    @Test
    @DisplayName("응답이 3초(테스트에서는 600ms) 뒤에 오면 시도는 제한 시간 500ms 근처에서 시간 초과로 끝난다")
    void slowResponseTimesOutNearTheAttemptLimit() {
        // Given 재고 서비스 자리가 예약 요청에 600ms 뒤에 응답한다. 시도당 제한 시간(테스트 설정 500ms)보다 길다.
        stubReserve(reserved().withFixedDelay(600));
        InventoryCallContext context = new InventoryCallContext();

        // When 재고 예약을 한 번 시도한다
        long started = System.nanoTime();
        InventoryCallFailure failure = null;
        try {
            inventoryClient.reserve(1L, List.of(new OrderItemRequest("A", 1)), context);
        } catch (InventoryCallFailure e) {
            failure = e;
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
        log.info("느린 응답에서 시도가 끝난 시간: {} ms", elapsed.toMillis());

        // Then 시도는 시간 초과로 끝난다. 응답을 받지 못하고 제한 시간 500ms 근처에서 끝난다.
        assertThat(failure).isNotNull();
        assertThat(failure.kind()).isEqualTo(InventoryCallFailure.Kind.TIMEOUT);
        assertThat(elapsed).isGreaterThanOrEqualTo(EARLIEST_TIMEOUT).isLessThan(LATEST_TIMEOUT);
        assertThat(context.sentAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("응답하지 않는 주소로 연결하면 연결 시간을 포함해 제한 시간 500ms 근처에서 끝난다")
    void unresponsiveAddressFailsWithinTheAttemptLimit() {
        // Given 연결에 응답하지 않는 주소가 있다. 문서용으로 예약된 주소라서 어디로도 라우팅되지 않고, 연결을 시도하면 기다리게 된다.
        String url = "http://" + UNRESPONSIVE_ADDRESS + ":81/reservations/1";

        // When 운영과 같은 설정의 재고 호출용 RestClient로 그 주소에 요청한다
        long started = System.nanoTime();
        assertThatThrownBy(() -> inventoryRestClient.put().uri(url).retrieve().toBodilessEntity())
                .isInstanceOf(ResourceAccessException.class);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
        log.info("응답하지 않는 주소에서 시도가 끝난 시간: {} ms", elapsed.toMillis());

        // Then 연결 제한 시간(500ms)과 요청 제한 시간(500ms)을 더한 1000ms가 아니라, 500ms 근처에서 끝난다
        assertThat(elapsed).isGreaterThanOrEqualTo(EARLIEST_TIMEOUT).isLessThan(LATEST_TIMEOUT);
    }
}
