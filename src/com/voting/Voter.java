package com.voting;

import java.time.LocalDate;

public class Voter {

    private final String voterId;
    private final String name;
    private boolean hasVoted;
    // Null for voters registered before DOB was recorded
    private LocalDate dateOfBirth;
    // Never the plaintext PIN; null means no PIN has been generated yet
    private String passwordHash;

    public Voter(String voterId, String name) {
        this(voterId, name, null);
    }

    public Voter(String voterId, String name, LocalDate dateOfBirth) {
        if (voterId == null || voterId.isBlank())
            throw new IllegalArgumentException("Voter ID must not be null or blank.");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Voter name must not be null or blank.");
        this.voterId    = voterId.trim();
        this.name       = name.trim();
        this.dateOfBirth = dateOfBirth;
        this.hasVoted   = false;
    }

    public String getVoterId() { return voterId; }
    public String getName()    { return name; }
    public boolean hasVoted()  { return hasVoted; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public boolean hasDateOfBirth() {
        return dateOfBirth != null;
    }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isBlank();
    }

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
