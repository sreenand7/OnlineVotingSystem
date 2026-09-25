package com.voting;

public class Voter {

    private final String voterId;
    private final String name;
    private boolean hasVoted;

    public Voter(String voterId, String name) {
        if (voterId == null || voterId.isBlank())
            throw new IllegalArgumentException("Voter ID must not be null or blank.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Voter name must not be null or blank.");
        this.voterId  = voterId.trim();
        this.name     = name.trim();
        this.hasVoted = false;
    }

    public String getVoterId() { return voterId; }
    public String getName()    { return name; }
    public boolean hasVoted()  { return hasVoted; }

    public void markAsVoted() {
        this.hasVoted = true;
    }

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
