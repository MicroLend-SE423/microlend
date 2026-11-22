package pk.edu.giki.microlend;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * A feature needs a module that has not been built yet. Thrown by the
 * placeholders the composition root uses until teammates' modules exist, so
 * the gap is reported by name instead of being papered over.
 */
public final class ModuleNotAvailableException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param module the missing module, for example {@code products}
     * @param owner the team member who owns it
     * @param needed what the caller needed from it
     */
    public ModuleNotAvailableException(final String module, final String owner,
                                       final String needed) {
        super(needed + " needs the " + module + " module (" + owner + "), which is not built yet");
    }
}
