package com.example.msa.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * 원격 호출을 DB 트랜잭션 안에서 하지 않는다. docs/standards/coding-conventions.md 3-1절, 3-8절.
 * {@code @Transactional}이 붙은 메서드와 클래스는 {@code client} 패키지의 클래스를 직접 부르지 않는다.
 * 다른 빈을 거쳐 부르는 경우는 잡지 못한다.
 */
public final class TransactionalRemoteCallRules {

    private static final String SPRING_TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
    private static final String JAKARTA_TRANSACTIONAL = "jakarta.transaction.Transactional";

    private TransactionalRemoteCallRules() {}

    /** {@code @Transactional}이 붙은 클래스와 메서드는 client 패키지를 쓰지 않는다. */
    public static ArchRule transactionalCodeDoesNotCallClient(String basePackage) {
        String clientPackage = basePackage + ".client";
        ArchRule classRule = noClasses()
                .that()
                .areAnnotatedWith(SPRING_TRANSACTIONAL)
                .or()
                .areAnnotatedWith(JAKARTA_TRANSACTIONAL)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(clientPackage + "..");
        ArchRule methodRule = methods()
                .that()
                .areAnnotatedWith(SPRING_TRANSACTIONAL)
                .or()
                .areAnnotatedWith(JAKARTA_TRANSACTIONAL)
                .should(notCallClient(clientPackage));
        return CompositeArchRule.of(classRule)
                .and(methodRule)
                .as("@Transactional이 붙은 메서드와 클래스는 client 패키지의 클래스를 직접 부르지 않는다");
    }

    private static ArchCondition<JavaMethod> notCallClient(String clientPackage) {
        return new ArchCondition<>("client 패키지의 클래스를 직접 부르지 않는다") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaCall<?> call : method.getCallsFromSelf()) {
                    JavaClass owner = call.getTargetOwner();
                    String packageName = owner.getPackageName();
                    if (packageName.equals(clientPackage) || packageName.startsWith(clientPackage + ".")) {
                        events.add(SimpleConditionEvent.violated(
                                method,
                                method.getFullName() + "이 트랜잭션 안에서 " + owner.getName() + "을 부른다. 원격 호출은 트랜잭션 밖에서 한다"));
                    }
                }
            }
        };
    }
}
