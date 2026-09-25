package com.voting;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Vote {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Voter         voter;
    private final Candidate     candidate;
    private final LocalDateTime timestamp;

    public Vote(Voter voter, Candidate candidate, LocalDateTime timestamp) {
        if (voter == null)     throw new IllegalArgumentException("Voter must not be null.");
        if (candidate == null) throw new IllegalArgumentException("Candidate must not be null.");
        if (timestamp == null) throw new IllegalArgumentException("Timestamp must not be null.");
        this.voter     = voter;
        this.candidate = candidate;
        this.timestamp = timestamp;
    }

    public Voter         getVoter()     { return voter; }
    public Candidate     getCandidate() { return candidate; }
    public LocalDateTime getTimestamp()  { return timestamp; }

    @Override
    public String toString() {
        return String.format("Vote[voter=%s, candidate=%s, time=%s]",
                voter.getVoterId(),
                candidate.getCandidateId(),
                timestamp.format(DISPLAY_FMT));
    }
}
