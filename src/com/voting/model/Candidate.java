package com.voting.model;

/**
 * Represents a candidate standing for election.
 *
 * <p>Candidates are immutable once registered. Vote tallying is performed
 * externally by {@link com.voting.manager.VotingManager}.</p>
 */
public class Candidate {

    private final String candidateId;
    private final String name;
    private final String politicalParty;

    /**
     * Constructs a new Candidate.
     *
     * @param candidateId  unique identifier (e.g. "C001")
     * @param name         full name of the candidate
     * @param politicalParty  name of the affiliated political party
     * @throws IllegalArgumentException if any parameter is null/blank
     */
    public Candidate(String candidateId, String name, String politicalParty) {
        if (candidateId == null || candidateId.isBlank()) {
            throw new IllegalArgumentException("Candidate ID must not be null or blank.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Candidate name must not be null or blank.");
        }
        if (politicalParty == null || politicalParty.isBlank()) {
            throw new IllegalArgumentException("Political party must not be null or blank.");
        }
        this.candidateId    = candidateId.trim();
        this.name           = name.trim();
        this.politicalParty = politicalParty.trim();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getCandidateId()   { return candidateId; }
    public String getName()          { return name; }
    public String getPoliticalParty(){ return politicalParty; }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("Candidate[id=%s, name='%s', party='%s']",
                candidateId, name, politicalParty);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Candidate)) return false;
        return candidateId.equals(((Candidate) o).candidateId);
    }

    @Override
    public int hashCode() {
        return candidateId.hashCode();
    }
}
