package com.syncevent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    @Test
    void domainMustNotDependOnSpringOrJpa() {
        JavaClasses classes = productionClasses();
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..");

        rule.check(classes);
    }

    @Test
    void domainMustNotDependOnApplicationApiOrInfrastructure() {
        JavaClasses classes = productionClasses();
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..application..", "..api..", "..infrastructure..");

        rule.check(classes);
    }

    @Test
    void applicationMustNotDependOnApi() {
        JavaClasses classes = productionClasses();
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..api..");

        rule.check(classes);
    }

    @Test
    void domainApplicationAndApiMustNotDependOnInfrastructure() {
        JavaClasses classes = productionClasses();
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..domain..", "..application..", "..api..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    void infrastructureMustNotDependOnApi() {
        JavaClasses classes = productionClasses();
        ArchRule rule = noClasses()
                .that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("..api..");

        rule.check(classes);
    }

    @Test
    void portsMustResideInDomainPackage() {
        JavaClasses classes = productionClasses();
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Port")
                .should().resideInAPackage("..domain..");

        rule.check(classes);
    }

    @Test
    void adaptersEntitiesAndRepositoriesMustResideInInfrastructurePackage() {
        JavaClasses classes = productionClasses();
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Adapter")
                .or().haveSimpleNameEndingWith("Entity")
                .or().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage("..infrastructure..");

        rule.check(classes);
    }

    @Test
    void apiMustNotDependOnPersistencePorts() {
        JavaClasses classes = productionClasses();
        ArchRule rule = noClasses()
                .that().resideInAPackage("..api..")
                .should().dependOnClassesThat()
                .haveSimpleNameEndingWith("PersistencePort");

        rule.check(classes);
    }

    @Test
    void allClassesMustResideInBoundedContextsOrShared() {
        JavaClasses classes = productionClasses();
        ArchRule rule = classes()
                .that().doNotHaveSimpleName("SynceventApplication")
                .should().resideInAnyPackage(
                        "com.syncevent.events..",
                        "com.syncevent.participants..",
                        "com.syncevent.registrations..",
                        "com.syncevent.attendance..",
                        "com.syncevent.certificates..",
                        "com.syncevent.users..",
                        "com.syncevent.audit..",
                        "com.syncevent.shared..");

        rule.check(classes);
    }

    private JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.syncevent");
    }
}
