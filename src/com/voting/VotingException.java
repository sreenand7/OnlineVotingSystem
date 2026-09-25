package com.voting;

public class VotingException extends Exception {

    public VotingException(String message) {
        super(message);
    }

    public static class AlreadyVotedException extends VotingException {
        public AlreadyVotedException(String voterId) {
            super("Voter '" + voterId + "' has already cast their ballot.");
        }
    }

    public static class ElectionClosedException extends VotingException {
        public ElectionClosedException(String detail) {
            super("Election is not currently open: " + detail);
        }
    }

    public static class VoterNotFoundException extends VotingException {
        public VoterNotFoundException(String voterId) {
            super("No registered voter found with ID '" + voterId + "'.");
        }
    }

    public static class CandidateNotFoundException extends VotingException {
        public CandidateNotFoundException(String candidateId) {
            super("No registered candidate found with ID '" + candidateId + "'.");
        }
    }
}
