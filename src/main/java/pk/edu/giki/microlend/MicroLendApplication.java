package pk.edu.giki.microlend;

import java.math.BigDecimal;
import pk.edu.giki.microlend.common.Money;

/**
 * Entry point for MicroLend.
 *
 * <p>At Milestone 1 this reports the build and exercises the domain kernel so
 * that {@code ./gradlew run} does something observable. The local web interface
 * and the module wiring arrive during Milestone 2; nothing in this project is
 * deployed, and everything runs on a developer's own machine.</p>
 */
public final class MicroLendApplication {

    private MicroLendApplication() {
        // Entry point only.
    }

    /**
     * Runs the application.
     *
     * @param args command-line arguments; none are read at this milestone
     */
    public static void main(final String[] args) {
        final Money principal = Money.of(new BigDecimal("50000.00"));
        final Money dailyPenalty = principal.percentage(new BigDecimal("0.05"));

        System.out.println("MicroLend 0.1.0-M1");
        System.out.println("Community microfinance loan servicing and delinquency engine");
        System.out.println();
        System.out.println("Domain kernel check");
        System.out.println("  principal            : " + principal);
        System.out.println("  penalty for one day  : " + dailyPenalty + "  (BR-27, 0.05%/day)");
        System.out.println();
        System.out.println("Modules arrive in Milestone 2: origination, products, ledger,");
        System.out.println("delinquency, reporting.");
    }
}
