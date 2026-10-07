package com.example.msa.inventory.architecture;

import com.example.msa.archrules.LayerRules;
import com.example.msa.archrules.MyBatisAnnotationRules;
import com.example.msa.archrules.TransactionalRemoteCallRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 재고 서비스의 구조 규칙을 매 빌드에서 검사한다. 규칙은 {@code libs/archunit-rules}에 있고, 이 서비스의 기본 패키지를 넘겨 적용한다.
 * 근거는 docs/standards/coding-conventions.md 2절 "의존 방향"과 3-8절 "기계로 검사하는 규칙"이다.
 */
@AnalyzeClasses(packages = ArchitectureTest.BASE_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String BASE_PACKAGE = "com.example.msa.inventory";

    /** controller는 mapper와 client를 직접 부르지 않는다. */
    @ArchTest
    static final ArchRule controllerDoesNotUseMapperOrClient =
            LayerRules.controllerDoesNotUseMapperOrClient(BASE_PACKAGE);

    /** mapper, client, domain은 service와 controller를 모른다. */
    @ArchTest
    static final ArchRule mapperClientDomainDoNotKnowServiceOrController =
            LayerRules.mapperClientDomainDoNotKnowServiceOrController(BASE_PACKAGE);

    /** domain은 Spring과 MyBatis를 모른다(순수 Java). */
    @ArchTest
    static final ArchRule domainDoesNotKnowSpringOrMyBatis = LayerRules.domainDoesNotKnowSpringOrMyBatis(BASE_PACKAGE);

    /** 애너테이션 SQL을 쓰지 않는다. */
    @ArchTest
    static final ArchRule noAnnotationSql = MyBatisAnnotationRules.noAnnotationSql();

    /**
     * 원격 호출을 트랜잭션 안에서 하지 않는다. 재고 서비스에는 client 패키지가 없고 {@code @Transactional}을 클래스가 아니라 메서드에 붙이므로,
     * 이 규칙의 대상 가운데 클래스 쪽이 비어 있다. ArchUnit은 대상이 비면 규칙을 실패로 보기 때문에 이 규칙에만 allowEmptyShould를 둔다
     * (2026-10-07 사용자 승인, 저장소 루트 CLAUDE.md 7절의 "ArchUnit 규칙 예외"). 나중에 재고 서비스에 client 패키지가 생기면 이 규칙이 바로 검사한다.
     */
    @ArchTest
    static final ArchRule transactionalCodeDoesNotCallClient =
            TransactionalRemoteCallRules.transactionalCodeDoesNotCallClient(BASE_PACKAGE)
                    .allowEmptyShould(true);
}
