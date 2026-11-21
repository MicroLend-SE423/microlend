package pk.edu.giki.microlend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Architecture rules checked on every {@code ./gradlew build} (queue item L-19).
 *
 * <p>C-03: no cyclic dependencies between module packages. The other rules
 * restate M1 design section 4.5: domain modules depend on abstractions they
 * declare, never on the persistence or web packages that implement or use
 * them, and {@code common} depends on no module.</p>
 */
class ArchitectureTest {

    private static final String ROOT = "pk.edu.giki.microlend";

    private static JavaClasses production;

    @BeforeAll
    static void importProductionClasses() {
        production = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
    }

    @Test
    @DisplayName("C-03: module packages are free of cycles")
    void noCyclesBetweenModules() {
        slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(production);
    }

    @Test
    @DisplayName("domain modules do not depend on persistence or web (DIP, section 4.5)")
    void domainDoesNotDependOnInfrastructure() {
        noClasses().that().resideInAnyPackage(ROOT + ".common..", ROOT + ".ledger..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(ROOT + ".persistence..", ROOT + ".web..")
            .check(production);
    }

    @Test
    @DisplayName("common depends on no module")
    void commonIsTheKernel() {
        noClasses().that().resideInAPackage(ROOT + ".common..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(ROOT + ".ledger..", ROOT + ".persistence..", ROOT + ".web..")
            .check(production);
    }
}
