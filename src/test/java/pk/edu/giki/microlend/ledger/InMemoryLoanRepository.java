package pk.edu.giki.microlend.ledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;

/** Test fake for {@link LoanRepository}; records every save. */
final class InMemoryLoanRepository implements LoanRepository {
    private final Map<LoanId, LoanAccount> accounts = new HashMap<>();
    private final List<LoanAccount> saves = new ArrayList<>();

    InMemoryLoanRepository with(final LoanAccount account) {
        accounts.put(account.id(), account);
        return this;
    }

    List<LoanAccount> saves() {
        return saves;
    }

    @Override
    public Optional<LoanAccount> findById(final LoanId loanId) {
        return Optional.ofNullable(accounts.get(Objects.requireNonNull(loanId)));
    }

    @Override
    public void save(final LoanAccount account) {
        accounts.put(account.id(), Objects.requireNonNull(account));
        saves.add(account);
    }
}
