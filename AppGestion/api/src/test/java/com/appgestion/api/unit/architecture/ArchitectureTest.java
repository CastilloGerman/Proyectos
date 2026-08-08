package com.appgestion.api.unit.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.base.DescribedPredicate.not;

/**
 * Reglas de capas acordes a Parte 2: reforzar límites sin exigir refactors masivos previos.
 * <p>
 * Deuda pendiente: {@code DevController} y {@code ResendWebhookController} aún inyectan
 * repositorios; migrar en sesiones separadas y retirar las exclusiones de
 * {@link #controllers_should_not_depend_on_repositories}.
 */
@AnalyzeClasses(packages = "com.appgestion.api")
class ArchitectureTest {

    @ArchTest
    void repositories_should_not_depend_on_controllers(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..repository..")
                .should().dependOnClassesThat().resideInAPackage("..controller..")
                .because("los repositorios son adaptadores de persistencia; no deben conocer la capa HTTP")
                .check(classes);
    }

    @ArchTest
    void controllers_should_not_depend_on_repositories(JavaClasses classes) {
        noClasses()
                .that().resideInAPackage("..controller..")
                .and(not(simpleName("DevController")))
                .and(not(simpleName("ResendWebhookController")))
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("los controllers delegan en servicios; no acceden a persistencia directamente")
                .check(classes);
    }

    @ArchTest
    void domain_should_not_depend_on_spring_web(JavaClasses classes) {
        noClasses()
                .that().resideInAnyPackage("..domain.entity..", "..domain.enums..", "..domain.presupuesto..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..")
                .because("el dominio no debe acoplarse a Spring Web")
                .check(classes);
    }
}
