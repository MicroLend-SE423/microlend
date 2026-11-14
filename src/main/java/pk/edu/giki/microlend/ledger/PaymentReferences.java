package pk.edu.giki.microlend.ledger;

/**
 * Role that remembers which (bank, reference) pairs have been posted (BR-25).
 *
 * <p>{@link #claim} must be atomic: when several threads claim the same pair
 * at once, exactly one succeeds.</p>
 */
public interface PaymentReferences {

    /**
     * Claims a pair for a posting about to be made.
     *
     * @param bank the reporting bank
     * @param reference the bank's transaction reference
     * @return true if the pair was free and is now claimed; false if already present
     */
    boolean claim(String bank, String reference);

    /**
     * Releases a pair whose posting failed, so a corrected retry is accepted.
     *
     * @param bank the reporting bank
     * @param reference the bank's transaction reference
     */
    void release(String bank, String reference);
}
