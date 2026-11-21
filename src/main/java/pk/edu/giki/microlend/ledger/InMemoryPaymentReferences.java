package pk.edu.giki.microlend.ledger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, process-local {@link PaymentReferences}.
 *
 * <p>Claims are atomic through {@link ConcurrentHashMap}. The set is lost when
 * the process stops, so a durable store backed by the {@code payment} table's
 * {@code UNIQUE (source_bank, bank_reference)} constraint is still to be
 * built; until then this class is the guard used by the composition root.</p>
 */
public final class InMemoryPaymentReferences implements PaymentReferences {

    private record Pair(String bank, String reference) { }

    private final Set<Pair> claimed = ConcurrentHashMap.newKeySet();

    @Override
    public boolean claim(final String bank, final String reference) {
        return claimed.add(pair(bank, reference));
    }

    @Override
    public void release(final String bank, final String reference) {
        claimed.remove(pair(bank, reference));
    }

    private static Pair pair(final String bank, final String reference) {
        return new Pair(Objects.requireNonNull(bank, "bank is required"),
            Objects.requireNonNull(reference, "reference is required"));
    }
}
