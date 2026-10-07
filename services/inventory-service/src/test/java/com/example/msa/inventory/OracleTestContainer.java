package com.example.msa.inventory;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.OracleContainer;

/**
 * 재고 서비스의 모든 테스트(Mapper 테스트, 서비스 통합 테스트)가 함께 쓰는 Oracle 컨테이너 하나다.
 * 로컬 실행 인프라와 같은 이미지(gvenzl/oracle-xe:21.3.0-slim-faststart)를 쓴다. H2 같은 대체 DB를 쓰지 않는다.
 *
 * <p>테스트 클래스에 {@code @ImportTestcontainers(OracleTestContainer.class)}를 붙여 쓴다.
 * {@code @ServiceConnection} 덕분에 DataSource와 Flyway가 이 컨테이너에 자동으로 연결된다.
 * 컨테이너는 처음 필요할 때 한 번 뜨고, 같은 JVM의 다른 테스트 클래스가 다시 쓴다.
 */
public interface OracleTestContainer {

    @ServiceConnection
    OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-xe:21.3.0-slim-faststart");
}
