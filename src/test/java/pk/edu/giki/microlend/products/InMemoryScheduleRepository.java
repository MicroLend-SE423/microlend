package pk.edu.giki.microlend.products;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;

/** Test fake of {@link ScheduleRepository}: schedules held in a map. */
public final class InMemoryScheduleRepository implements ScheduleRepository {

    private final Map<LoanId, Schedule> schedules = new HashMap<>();

    @Override
    public void save(final LoanId loanId, final Schedule schedule) {
        if (schedules.putIfAbsent(loanId, schedule) != null) {
            throw new InvalidScheduleException("loan " + loanId + " already has a schedule");
        }
    }

    @Override
    public Optional<Schedule> scheduleFor(final LoanId loanId) {
        return Optional.ofNullable(schedules.get(loanId));
    }
}
