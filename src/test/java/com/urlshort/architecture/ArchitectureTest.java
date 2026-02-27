package com.urlshort.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.base.DescribedPredicate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Architecture Rules")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.urlshort");
    }

    @Test
    @DisplayName("No @Autowired field injection allowed")
    void noFieldInjection() {
        noFields()
                .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .because("Use constructor injection via @RequiredArgsConstructor instead of @Autowired field injection")
                .check(classes);
    }

    @Test
    @DisplayName("Controllers must not access repositories directly")
    void controllersCannotAccessRepositories() {
        noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("Controllers should delegate to services, not access repositories directly")
                .check(classes);
    }

    @Test
    @DisplayName("No service/impl package should exist")
    void noServiceImplPackage() {
        noClasses()
                .should().resideInAPackage("com.urlshort.service.impl")
                .because("Service interfaces/impl pattern was removed — use concrete @Service classes")
                .check(classes);
    }

    @Test
    @DisplayName("No @Transactional on controllers")
    void noTransactionalOnControllers() {
        noClasses()
                .that().resideInAPackage("..controller..")
                .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                .because("@Transactional belongs on service layer, not controllers")
                .check(classes);
    }

    @Test
    @DisplayName("No generic exceptions in service/controller/event layers")
    void noGenericExceptionsInBusinessCode() {
        DescribedPredicate<JavaConstructorCall> isGenericException = new DescribedPredicate<>("constructor of generic exception") {
            @Override
            public boolean test(JavaConstructorCall call) {
                String targetName = call.getTargetOwner().getName();
                return targetName.equals(IllegalArgumentException.class.getName())
                        || targetName.equals(IllegalStateException.class.getName())
                        || targetName.equals(RuntimeException.class.getName());
            }
        };

        noClasses()
                .that().resideInAnyPackage("..service..", "..controller..", "..event..")
                .should().callConstructorWhere(isGenericException)
                .because("Use custom exceptions from com.urlshort.exception package instead of generic Java exceptions")
                .check(classes);
    }

    @Test
    @DisplayName("Layered architecture: controller -> service -> repository")
    void layeredArchitectureIsRespected() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")
                .layer("Event").definedBy("..event..")
                .layer("Security").definedBy("..security..")
                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service", "Event", "Security")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Event", "Security")
                .check(classes);
    }

    @Test
    @DisplayName("Services in service package should be annotated with @Service")
    void servicesMustBeAnnotated() {
        classes()
                .that().resideInAPackage("..service..")
                .and().areNotInterfaces()
                .and().areNotEnums()
                .and().areNotRecords()
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .because("Service classes must be annotated with @Service for Spring component scanning")
                .check(classes);
    }
}
