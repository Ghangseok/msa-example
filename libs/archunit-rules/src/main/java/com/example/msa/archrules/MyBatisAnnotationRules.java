package com.example.msa.archrules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

/**
 * 애너테이션 SQL을 쓰지 않는다. docs/standards/coding-conventions.md 3-2절, 3-8절.
 * SQL은 Mapper XML에 쓴다. 그래야 SQL을 한곳에서 찾고 기계 검사가 Mapper XML을 읽을 수 있다.
 */
public final class MyBatisAnnotationRules {

    private static final String ANNOTATION_PACKAGE = "org.apache.ibatis.annotations.";

    private static final List<String> SQL_ANNOTATIONS = List.of(
            "Select",
            "Insert",
            "Update",
            "Delete",
            "SelectProvider",
            "InsertProvider",
            "UpdateProvider",
            "DeleteProvider");

    private MyBatisAnnotationRules() {}

    /** @Select, @Insert, @Update, @Delete, @SelectProvider, @InsertProvider, @UpdateProvider, @DeleteProvider를 쓰지 못하게 한다. */
    public static ArchRule noAnnotationSql() {
        return methods().should(notBeAnnotatedWithSql()).as("애너테이션 SQL(@Select 등)을 쓰지 않는다");
    }

    private static ArchCondition<JavaMethod> notBeAnnotatedWithSql() {
        return new ArchCondition<>("애너테이션 SQL을 쓰지 않는다") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (String name : SQL_ANNOTATIONS) {
                    if (method.isAnnotatedWith(ANNOTATION_PACKAGE + name)) {
                        events.add(SimpleConditionEvent.violated(
                                method, method.getFullName() + "에 @" + name + "가 있다. SQL은 Mapper XML에 쓴다"));
                    }
                }
            }
        };
    }
}
