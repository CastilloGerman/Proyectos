package com.appgestion.api.unit.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de capas acordes a Parte 2: reforzar límites sin exigir refactors masivos previos.
 * <p>
 * Nota: varios controllers inyectan repositorios (webhooks, dev, auditoría). Eso sigue siendo
 * deuda técnica documentada en la auditoría Parte 1; aquí no se añade regla que falle hasta
 * mover esos casos a servicios.
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
    void domain_should_not_depend_on_spring_web(JavaClasses classes) {
        noClasses()
                .that().resideInAnyPackage("..domain.entity..", "..domain.enums..", "..domain.presupuesto..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..")
                .because("el dominio no debe acoplarse a Spring Web")
                .check(classes);
    }
}
