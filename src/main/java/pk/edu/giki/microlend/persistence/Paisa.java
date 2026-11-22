package pk.edu.giki.microlend.persistence;

import java.math.BigDecimal;
import pk.edu.giki.microlend.common.Money;

/**
 * Converts {@link Money} to and from the {@code INTEGER} minor units (paisa)
 * that every monetary column holds (constraint C-04).
 */
final class Paisa {

    private Paisa() {
    }

    static long of(final Money amount) {
        return new BigDecimal(amount.toPlainString()).movePointRight(Money.SCALE).longValueExact();
    }

    static Money toMoney(final long paisa) {
        return Money.of(BigDecimal.valueOf(paisa, Money.SCALE));
    }
}
