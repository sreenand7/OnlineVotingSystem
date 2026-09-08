package com.voting.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * An immutable record of a single ballot cast during the election.
 *
 * <p>Instances are created by {@link com.voting.manager.VotingManager}
 * only after all validation checks pass. Once created a Vote cannot be
 * modified, ensuring auditability.</p>
 */
public final class Vote {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Voter         voter;
    private final Candidate     candidate;
    private final LocalDateTime timestamp;

    /**
     * Constructs a Vote record.
     *
     * @param voter     the voter who cast this ballot
     * @param candidate the candidate who received this vote
     * @param timestamp the exact moment the vote was recorded
     * @throws IllegalArgumentException if any argument is null
     */
    public Vote(Voter voter, Candidate candidate, LocalDateTime timestamp) {
        if (voter == null)     throw new IllegalArgumentException("Voter must not be null.");
        if (candidate == null) throw new IllegalArgumentException("Candidate must not be null.");
        if (timestamp == null) throw new IllegalArgumentException("Timestamp must not be null.");
        this.voter     = voter;
        this.candidate = candidate;
        this.timestamp = timestamp;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public Voter         getVoter()     { return voter; }
    public Candidate     getCandidate() { return candidate; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Vote[voter=%s, candidate=%s, time=%s]",
                voter.getVoterId(),
                candidate.getCandidateId(),
                timestamp.format(DISPLAY_FMT));
    }
}
