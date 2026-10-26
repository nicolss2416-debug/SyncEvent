package com.syncevent;

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

    private JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.syncevent");
    }
}
