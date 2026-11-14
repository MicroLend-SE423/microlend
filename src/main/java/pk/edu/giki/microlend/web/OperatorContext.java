package pk.edu.giki.microlend.web;

import java.util.function.Supplier;

/**
 * The operator of the request being handled on the current thread.
 *
 * <p>The teller audit decorator asks a {@code Supplier<String>} for the
 * operator (FR-31); the web server sets it for exactly the duration of one
 * posting, so one teller's name never leaks into another request.</p>
 */
public final class OperatorContext implements Supplier<String> {

    private final ThreadLocal<String> current = new ThreadLocal<>();

    /**
     * Returns the operator of the current request.
     *
     * @return the operator, or null outside a request
     */
    @Override
    public String get() {
        return current.get();
    }

    /**
     * Runs an action as the given operator, then clears the operator.
     *
     * @param operator the operator named on the form
     * @param action what to run
     * @param <T> the action's result type
     * @return the action's result
     */
    public <T> T runAs(final String operator, final Supplier<T> action) {
        current.set(operator);
        try {
            return action.get();
        } finally {
            current.remove();
        }
    }
}
