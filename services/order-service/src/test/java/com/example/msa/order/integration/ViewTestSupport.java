package com.example.msa.order.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.example.msa.order.domain.OrderReason;
import com.example.msa.order.domain.OrderStatus;
import com.example.msa.order.domain.ReleaseStatus;
import com.example.msa.order.dto.OrderWithItems;
import com.example.msa.order.mapper.OrderMapper;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 주문 조회의 서비스 통합 테스트가 함께 쓰는 준비 코드다. 조회는 재고 서비스를 부르지 않으므로 WireMock 없이
 * {@link IntegrationTestSupport}를 바로 상속한다.
 *
 * <ul>
 *   <li>"주문이 있다"는 상태는 주문 생성 API가 아니라 {@link OrderMapper}로 직접 저장해서 만든다({@link #seed}).
 *       주문 번호는 시퀀스가 정하므로, 테스트 케이스 문서의 "주문 1004" 같은 번호는 저장 결과의 번호로 대신한다.
 *   <li>관리자 M1은 {@link #admin}으로 흉내 낸다. Spring Security Test의 {@code jwt()}는 역할 변환기를 거치지 않으므로 권한을 직접 준다.
 * </ul>
 *
 * <p>이 클래스는 기대값을 정하지 않는다. 기대값은 각 테스트가 {@code docs/test-cases/order-placement.md}에서 그대로 가져온다.
 * 아직 없는 구현 클래스를 참조하지 않는다. 응답은 JSON 글자로 읽는다. 그래야 구현 전의 테스트가 컴파일 오류가 아니라 단언 실패로 끝난다.
 */
public abstract class ViewTestSupport extends IntegrationTestSupport {

    /** 관리자 M1의 JWT {@code sub} 값이다. 테스트 안에서 정한 문자열이다. */
    protected static final String M1 = "admin-m1";

    private static final Logger log = LoggerFactory.getLogger(ViewTestSupport.class);

    @Autowired
    private OrderMapper orderMapper;

    /** 관리자 역할만 있는 사용자를 JWT로 흉내 낸다. {@code sub}가 사용자 ID이고, 권한은 {@code ROLE_ADMIN} 하나다. */
    protected static RequestPostProcessor admin(String sub) {
        return jwt().jwt(token -> token.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    /** 저장할 주문 항목 하나. 거절 사유는 없다. */
    protected static OrderWithItems.StoredItem item(String productId, int quantity) {
        return new OrderWithItems.StoredItem(productId, quantity, null);
    }

    /** 저장할 주문 항목 하나. {@code rejectReason}은 거절된 주문에서 이 상품이 부족했는지(OUT_OF_STOCK), 없었는지(PRODUCT_NOT_FOUND)다. */
    protected static OrderWithItems.StoredItem item(String productId, int quantity, OrderReason rejectReason) {
        return new OrderWithItems.StoredItem(productId, quantity, rejectReason);
    }

    /**
     * 주문을 직접 저장하고 주문 번호를 돌려준다. 주문 생성이 쓰는 SQL을 그대로 쓴다.
     * 처리중으로 넣은 뒤 항목별 거절 사유를 채우고, 상태가 처리중이 아니면 결과(상태, 사유, 해제 결과)를 저장한다.
     * 사유와 해제 결과가 없으면 null을 넘긴다.
     */
    protected long seed(
            String customerId,
            OrderStatus status,
            OrderReason reason,
            ReleaseStatus releaseStatus,
            OrderWithItems.StoredItem... items) {
        OrderWithItems order = new OrderWithItems();
        order.setCustomerId(customerId);
        order.setRequestKey(newKey());
        order.setStatus(OrderStatus.PENDING);
        orderMapper.insertOrder(order);
        long orderNo = order.getOrderNo();
        for (OrderWithItems.StoredItem stored : items) {
            orderMapper.insertOrderItem(orderNo, stored.productId(), stored.quantity());
            if (stored.rejectReason() != null) {
                orderMapper.updateItemRejectReason(orderNo, stored.productId(), stored.rejectReason());
            }
        }
        if (status != OrderStatus.PENDING) {
            orderMapper.updateOrderResult(orderNo, status, reason, releaseStatus);
        }
        return orderNo;
    }

    /** GET 요청을 보낸다. {@code auth}가 null이면 토큰 없이 보낸다. 쿼리 문자열은 {@code path}에 붙여서 넘긴다. */
    protected Reply get(RequestPostProcessor auth, String path) {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(path);
        if (auth != null) {
            request.with(auth);
        }
        try {
            MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
            return new Reply(
                    response.getStatus(),
                    response.getContentType(),
                    response.getContentAsString(StandardCharsets.UTF_8));
        } catch (ServletException e) {
            // 처리되지 않은 예외다. MockMvc는 예외를 밖으로 던지지만, 실제 서버에서는 500 응답이 된다.
            log.warn("GET {}가 처리되지 않은 예외로 끝났다. 500으로 본다", path, e);
            return new Reply(500, null, "");
        } catch (Exception e) {
            throw new IllegalStateException("GET " + path + " 요청을 보내지 못했다", e);
        }
    }

    /** 주문 하나 응답의 {@code items}를 나온 순서대로 읽는다. */
    protected static List<Item> itemsOf(Reply reply) {
        List<?> values = JsonPath.read(reply.body(), "$.items[*]");
        return values.stream()
                .map(value -> {
                    Map<?, ?> line = (Map<?, ?>) value;
                    return new Item(String.valueOf(line.get("productId")), ((Number) line.get("quantity")).intValue());
                })
                .toList();
    }

    /** 목록 응답의 {@code orders}에서 주문 번호를 나온 순서대로 읽는다. */
    protected static List<Long> orderNos(Reply reply) {
        List<?> values = JsonPath.read(reply.body(), "$.orders[*].orderNo");
        return values.stream().map(value -> ((Number) value).longValue()).toList();
    }
}
