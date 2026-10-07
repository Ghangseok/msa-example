package com.example.msa.inventory.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.msa.inventory.OracleTestContainer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 시나리오 9.1의 재고 쪽: 예약으로 줄인 수량을 해제로 되돌리는 SQL이 맞게 도는지 본다. 테스트가 끝나면 변경은 롤백된다.
 *
 * <p>기대값의 출처는 도메인 분석 6절 표의 5번("해제 요청, 예약됨 상태: 수량을 복구하고 해제됨으로 바꾼다")과
 * 테스트 케이스 문서의 "상품 A의 재고는 10개다"이다. 상품 A가 7개일 때 3개를 되돌리면 10개가 된다.
 * User Story 9의 작업이 User Story 1의 파일을 고치지 않도록 StockMapperTest와 파일을 나눴다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(OracleTestContainer.class)
@ActiveProfiles("test")
class StockRestoreMapperTest {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("상품 A 7개 행에 3개를 되돌리면 10개가 된다")
    void increasesQuantity() {
        jdbc.update("INSERT INTO STOCK (PRODUCT_ID, QUANTITY) VALUES ('A', 7)");

        assertThat(stockMapper.updateIncreaseQuantity("A", 3)).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT QUANTITY FROM STOCK WHERE PRODUCT_ID = 'A'", Integer.class))
                .isEqualTo(10);
    }
}
