package com.example.msa.inventory.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.msa.inventory.OracleTestContainer;
import com.example.msa.inventory.dto.StockRow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 재고 행의 잠금과 감소 SQL이 실제 Oracle에서 맞게 도는지 본다.
 *
 * <p>기대값의 출처는 설계 문서 6절 "예약 처리" 2번과 그 아래 설명이다.
 * "재고 행을 상품 ID 순서로 하나씩 SELECT ... FOR UPDATE로 잠그고 읽는다", "행 잠금으로 동시 예약의 초과 판매를 막고"이다.
 * 잠금을 보려면 트랜잭션이 여러 개여야 하므로, 이 테스트는 테스트 전체를 트랜잭션으로 감싸지 않고 트랜잭션을 직접 연다.
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportTestcontainers(OracleTestContainer.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockMapperTest {

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void insertStockOfA() {
        deleteTestRows();
        jdbc.update("INSERT INTO STOCK (PRODUCT_ID, QUANTITY) VALUES ('A', 10)");
    }

    @AfterEach
    void deleteTestRows() {
        jdbc.update("DELETE FROM STOCK WHERE PRODUCT_ID IN ('A', 'Z')");
    }

    @Test
    @DisplayName("상품 A 10개 행을 잠그고 읽은 뒤 1개 줄이면 9개가 된다")
    void locksAndReadsRowThenDecreases() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            StockRow row = stockMapper.selectStockForUpdate("A");
            assertThat(row.productId()).isEqualTo("A");
            assertThat(row.quantity()).isEqualTo(10);

            assertThat(stockMapper.updateDecreaseQuantity("A", 1)).isEqualTo(1);

            assertThat(stockMapper.selectStockForUpdate("A").quantity()).isEqualTo(9);
        });
        // 커밋한 뒤에도 9개다
        assertThat(jdbc.queryForObject("SELECT QUANTITY FROM STOCK WHERE PRODUCT_ID = 'A'", Integer.class))
                .isEqualTo(9);
    }

    @Test
    @DisplayName("재고 데이터에 없는 상품(Z)을 읽으면 결과가 없다")
    void missingProductReturnsNothing() {
        StockRow row =
                new TransactionTemplate(transactionManager).execute(status -> stockMapper.selectStockForUpdate("Z"));

        assertThat(row).isNull();
    }

    @Test
    @DisplayName("한 트랜잭션이 행을 잠그고 있는 동안 다른 트랜잭션의 같은 행 잠금은 기다린다")
    void lockWaitsWhileAnotherTransactionHoldsRow() throws InterruptedException, ExecutionException, TimeoutException {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch holderLocked = new CountDownLatch(1);
        CountDownLatch holderMayCommit = new CountDownLatch(1);
        try {
            // 첫 트랜잭션이 행을 잠그고 커밋하지 않은 채 기다린다
            Future<?> holder = executor.submit(() -> transaction.executeWithoutResult(status -> {
                stockMapper.selectStockForUpdate("A");
                holderLocked.countDown();
                awaitOrFail(holderMayCommit);
            }));
            assertThat(holderLocked.await(30, TimeUnit.SECONDS)).isTrue();

            // 둘째 트랜잭션이 같은 행을 잠그려 한다
            Future<StockRow> waiter =
                    executor.submit(() -> transaction.execute(status -> stockMapper.selectStockForUpdate("A")));

            // 첫 트랜잭션이 잠금을 쥐고 있는 동안에는 끝나지 않는다
            assertThatThrownBy(() -> waiter.get(2, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);

            // 첫 트랜잭션이 커밋하면 둘째가 잠금을 얻어 읽는다
            holderMayCommit.countDown();
            holder.get(30, TimeUnit.SECONDS);
            assertThat(waiter.get(30, TimeUnit.SECONDS).quantity()).isEqualTo(10);
        } finally {
            holderMayCommit.countDown();
            executor.shutdownNow();
        }
    }

    private static void awaitOrFail(CountDownLatch latch) {
        try {
            if (!latch.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("60초 안에 신호가 오지 않았다");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("기다리다 중단됐다", e);
        }
    }
}
