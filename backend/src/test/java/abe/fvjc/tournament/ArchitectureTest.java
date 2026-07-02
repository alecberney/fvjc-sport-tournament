package abe.fvjc.tournament;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

class ArchitectureTest {
    private static final JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("abe.fvjc.tournament");
    private static final String DOMAIN = "..domain..";
    private static final String API = "..api..";
    private static final String PERSISTENCE = "..persistence..";

    // === Layer dependencies ===

    @Test
    void domainShouldNotDependOnApi() {
        noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage(API)
            .because("domain must not import api")
            .check(CLASSES);
    }

    @Test
    void domainShouldNotDependOnPersistence() {
        noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAPackage(PERSISTENCE)
            .because("domain must not import persistence")
            .check(CLASSES);
    }

    @Test
    void apiShouldNotDependOnPersistence() {
        noClasses().that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAPackage(PERSISTENCE)
            .because("api must not import persistence")
            .check(CLASSES);
    }

    // === Package placement ===

    @Test
    void servicesShouldResideInDomainPackages() {
        classes().that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(DOMAIN)
            .because("@Service classes belong in domain packages")
            .check(CLASSES);
    }

    @Test
    void controllersShouldResideInApiPackages() {
        classes().that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage(API)
            .because("@RestController classes belong in api packages")
            .check(CLASSES);
    }

    @Test
    void repositoriesShouldResideInPersistencePackages() {
        classes().that().areAnnotatedWith(Repository.class)
            .should().resideInAPackage(PERSISTENCE)
            .because("@Repository classes belong in persistence packages")
            .check(CLASSES);
    }

    // === Visibility ===

    @Test
    void jpaEntitiesShouldNotBePublic() {
        classes().that().areAnnotatedWith(Entity.class)
            .should().notBePublic()
            .because("JPA entities must not leak outside the persistence package")
            .check(CLASSES);
    }

    @Test
    void springDataRepositoriesShouldNotBePublic() {
        classes().that().haveSimpleNameEndingWith("Repository")
            .and().resideInAPackage(PERSISTENCE)
            .should().notBePublic()
            .because("Spring Data repositories must not leak outside the persistence package")
            .check(CLASSES);
    }

    // === Transactional ===

    @Test
    void transactionalShouldNotBeOnServiceClasses() {
        noClasses().that().areAnnotatedWith(Service.class)
            .should().beAnnotatedWith(Transactional.class)
            .because("@Transactional belongs only on JpaXxxStore methods in the persistence layer")
            .check(CLASSES);
    }

    @Test
    void transactionalShouldNotBeOnServiceMethods() {
        noMethods().that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
            .should().beAnnotatedWith(Transactional.class)
            .because("@Transactional belongs only on JpaXxxStore methods in the persistence layer")
            .check(CLASSES);
    }

    @Test
    void transactionalShouldNotBeOnControllerClasses() {
        noClasses().that().areAnnotatedWith(RestController.class)
            .should().beAnnotatedWith(Transactional.class)
            .because("@Transactional belongs only on JpaXxxStore methods in the persistence layer")
            .check(CLASSES);
    }

    // === UtilityClass proxies — @UtilityClass generates final classes ===

    @Test
    void validatorsShouldBeFinal() {
        classes().that().haveSimpleNameEndingWith("Validator")
            .and().resideInAPackage(DOMAIN)
            .should().haveModifier(JavaModifier.FINAL)
            .because("validators use @UtilityClass which makes them final")
            .allowEmptyShould(true)
            .check(CLASSES);
    }

    @Test
    void apiMappersShouldBeFinal() {
        classes().that().haveSimpleNameEndingWith("ApiMapper")
            .should().haveModifier(JavaModifier.FINAL)
            .because("API mappers use @UtilityClass which makes them final")
            .check(CLASSES);
    }

    @Test
    void dbMappersShouldBeFinal() {
        classes().that().haveSimpleNameEndingWith("DbMapper")
            .should().haveModifier(JavaModifier.FINAL)
            .because("DB mappers use @UtilityClass which makes them final")
            .check(CLASSES);
    }

    // === DTO placement ===
    // Note: @Jacksonized has @Retention(SOURCE) and is not verifiable with ArchUnit.

    @Test
    void dtosShouldResideInApiPackages() {
        classes().that().haveSimpleNameEndingWith("Dto")
            .should().resideInAPackage(API)
            .because("DTOs belong in api packages")
            .check(CLASSES);
    }
}
