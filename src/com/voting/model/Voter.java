package com.voting.model;

/**
 * Represents a registered voter in the election system.
 *
 * <p>Each voter has a unique ID and name. The {@code hasVoted} flag
 * enforces the one-vote-per-voter rule across the entire election.</p>
 */
public class Voter {

    private final String voterId;
    private final String name;
    private boolean hasVoted;

    /**
     * Constructs a new Voter.
     *
     * @param voterId unique identifier for this voter (e.g. "V001")
     * @param name    full name of the voter
     * @throws IllegalArgumentException if voterId or name is null/blank
     */
    public Voter(String voterId, String name) {
        if (voterId == null || voterId.isBlank()) {
            throw new IllegalArgumentException("Voter ID must not be null or blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Voter name must not be null or blank.");
        }
        this.voterId  = voterId.trim();
        this.name     = name.trim();
        this.hasVoted = false;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getVoterId() { return voterId; }
    public String getName()    { return name; }
    public boolean hasVoted()  { return hasVoted; }

    // ── State mutation ────────────────────────────────────────────────────────

    /**
     * Marks this voter as having cast their ballot.
     * Called by {@link com.voting.manager.VotingManager} after a valid vote.
     */
    public void markAsVoted() {
        this.hasVoted = true;
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Voter[id=%s, name='%s', hasVoted=%s]",
                voterId, name, hasVoted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Voter)) return false;
        return voterId.equals(((Voter) o).voterId);
    }

    @Override
    public int hashCode() {
        return voterId.hashCode();
    }
}
