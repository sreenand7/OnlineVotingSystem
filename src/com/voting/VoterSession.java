package com.voting;

/**
 * Holds the identity of the voter who is currently logged in.
 * Shared across the whole application so it survives switching between elections.
 */
public class VoterSession {

    private static final VoterSession INSTANCE = new VoterSession();

    private int voterId = -1;
    private String voterName;

    public static VoterSession getInstance() {
        return INSTANCE;
    }

    public boolean isLoggedIn() {
        return voterName != null;
    }

    public int getVoterId() {
        if (!isLoggedIn())
            throw new IllegalStateException("No voter is currently logged in.");
        return voterId;
    }

    public String getVoterName() {
        return voterName;
    }

    void start(int voterId, String voterName) {
        this.voterId = voterId;
        this.voterName = voterName;
    }

    public void clear() {
        this.voterId = -1;
        this.voterName = null;
    }
}
