package com.voting.util;

/**
 * Hierarchy of checked exceptions for domain-level voting errors.
 *
 * <p>Using a typed hierarchy lets callers react differently to each
 * failure reason without inspecting message strings.</p>
 *
 * <pre>
 * VotingException
 * ├── AlreadyVotedException      – voter already cast a ballot
 * ├── ElectionClosedException    – voting window is not open
 * ├── VoterNotFoundException     – no voter with given ID
 * └── CandidateNotFoundException – no candidate with given ID
 * </pre>
 */
public class VotingException extends Exception {

    public VotingException(String message) {
        super(message);
    }

    // ── Concrete subtypes ─────────────────────────────────────────────────────

    /** Thrown when a voter attempts to vote a second time. */
    public static class AlreadyVotedException extends VotingException {
        public AlreadyVotedException(String voterId) {
            super("Voter '" + voterId + "' has already cast their ballot.");
        }
    }

    /** Thrown when a vote is attempted outside the configured time window. */
    public static class ElectionClosedException extends VotingException {
        public ElectionClosedException(String detail) {
            super("Election is not currently open: " + detail);
        }
    }

    /** Thrown when looking up a voter ID that is not registered. */
    public static class VoterNotFoundException extends VotingException {
        public VoterNotFoundException(String voterId) {
            super("No registered voter found with ID '" + voterId + "'.");
        }
    }

    /** Thrown when looking up a candidate ID that is not registered. */
    public static class CandidateNotFoundException extends VotingException {
        public CandidateNotFoundException(String candidateId) {
            super("No registered candidate found with ID '" + candidateId + "'.");
        }
    }
}
