package com.example.msa.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * 계층 사이의 의존 방향 규칙이다. docs/standards/coding-conventions.md 2절 "의존 방향"의 글을 그대로 옮겼다.
 *
 * <pre>
 * controller → service → mapper
 *                      → client
 *                      → domain
 * dto는 어디서나 쓸 수 있다
 * </pre>
 */
public final class LayerRules {

    private LayerRules() {}

    /** controller는 mapper와 client를 직접 부르지 않는다. */
    public static ArchRule controllerDoesNotUseMapperOrClient(String basePackage) {
        return noClasses()
                .that()
                .resideInAPackage(basePackage + ".controller..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(basePackage + ".mapper..", basePackage + ".client..")
                .as("controller는 mapper와 client를 직접 부르지 않는다");
    }

    /** mapper, client, domain은 service와 controller를 모른다. */
    public static ArchRule mapperClientDomainDoNotKnowServiceOrController(String basePackage) {
        return noClasses()
                .that()
                .resideInAnyPackage(basePackage + ".mapper..", basePackage + ".client..", basePackage + ".domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(basePackage + ".service..", basePackage + ".controller..")
                .as("mapper, client, domain은 service와 controller를 모른다");
    }

    /** domain은 Spring과 MyBatis를 모른다(순수 Java). */
    public static ArchRule domainDoesNotKnowSpringOrMyBatis(String basePackage) {
        return noClasses()
                .that()
                .resideInAPackage(basePackage + ".domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "org.mybatis..", "org.apache.ibatis..")
                .as("domain은 Spring과 MyBatis를 모른다(순수 Java)");
    }
}
