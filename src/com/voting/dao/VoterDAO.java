package com.voting.dao;

import com.voting.database.DatabaseConnection;
import com.voting.model.Voter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access class for the {@code voters} and {@code election_voters} tables.
 *
 * <p>A voter's identity ({@code voters}) is separate from their per-election
 * voting status ({@code election_voters}), since the same voter may take
 * part in multiple elections independently. This DAO performs database
 * operations only — validation and workflow decisions belong elsewhere.</p>
 *
 * <p>Note: {@link Voter#hasVoted()} reflects only the in-memory flag set at
 * construction (always {@code false} for voters reconstructed here), since
 * "has voted" is election-specific in this schema. Use
 * {@link #hasVoted(int, int)} to check per-election voting status.</p>
 */
public class VoterDAO {

    /**
     * Registers a new voter in the {@code voters} table.
     *
     * @param voterId unique voter ID
     * @param name    voter's full name
     * @throws SQLException if the insert fails (e.g. duplicate voter_id)
     */
    public void registerVoter(int voterId, String name) throws SQLException {
        String sql = "INSERT INTO voters (voter_id, name) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, voterId);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    /**
     * Finds a voter by ID.
     *
     * @param voterId the voter ID to look up
     * @return a {@link Voter} built from the stored name, or {@code null} if not found
     * @throws SQLException if the query fails
     */
    public Voter findByVoterId(int voterId) throws SQLException {
        String sql = "SELECT voter_id, name FROM voters WHERE voter_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, voterId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Voter(String.valueOf(rs.getInt("voter_id")), rs.getString("name"));
                }
                return null;
            }
        }
    }

    /**
     * Returns all registered voters.
     *
     * @return list of all voters (empty list if none)
     * @throws SQLException if the query fails
     */
    public List<Voter> getAllVoters() throws SQLException {
        String sql = "SELECT voter_id, name FROM voters";

        List<Voter> voters = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                voters.add(new Voter(String.valueOf(rs.getInt("voter_id")), rs.getString("name")));
            }
        }
        return voters;
    }

    /**
     * Returns voters enrolled in one election, ordered by voter ID.
     * The returned local {@link Voter#hasVoted()} state is not an
     * election-specific status indicator; use {@link #hasVoted(int, int)}.
     */
    public List<Voter> getVotersForElection(int electionId) throws SQLException {
        String sql = "SELECT v.voter_id, v.name "
                + "FROM voters v "
                + "JOIN election_voters ev ON v.voter_id = ev.voter_id "
                + "WHERE ev.election_id = ? "
                + "ORDER BY v.voter_id";
        List<Voter> voters = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, electionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    voters.add(new Voter(String.valueOf(rs.getInt("voter_id")),
                            rs.getString("name")));
                }
            }
        }
        return voters;
    }

    /**
     * Checks whether a voter with the given ID exists.
     *
     * @param voterId the voter ID to check
     * @return {@code true} if the voter exists
     * @throws SQLException if the query fails
     */
    public boolean voterExists(int voterId) throws SQLException {
        String sql = "SELECT 1 FROM voters WHERE voter_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, voterId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Enrolls an existing voter into a specific election via the
     * {@code election_voters} join table. {@code has_voted} starts as FALSE.
     *
     * @param electionId the election to enroll the voter in
     * @param voterId    the voter to enroll
     * @throws SQLException if the insert fails (e.g. voter already enrolled
     *                       in this election, or foreign key violation)
     */
    public void addVoterToElection(int electionId, int voterId) throws SQLException {
        String sql = "INSERT INTO election_voters (election_id, voter_id, has_voted) "
                + "VALUES (?, ?, FALSE)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, voterId);
            stmt.executeUpdate();
        }
    }

    /**
     * Checks whether a voter has already voted in a specific election.
     *
     * @param electionId the election to check
     * @param voterId    the voter to check
     * @return {@code true} if {@code has_voted} is TRUE for this voter in this election
     * @throws SQLException if the query fails, or the voter is not enrolled
     *                       in this election (no matching row)
     */
    public boolean hasVoted(int electionId, int voterId) throws SQLException {
        String sql = "SELECT has_voted FROM election_voters WHERE election_id = ? AND voter_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, voterId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("has_voted");
                }
                throw new SQLException(
                        "Voter " + voterId + " is not enrolled in election " + electionId + ".");
            }
        }
    }

    /**
     * Marks a voter as having voted in a specific election.
     *
     * @param electionId the election in which the voter voted
     * @param voterId    the voter who voted
     * @throws SQLException if the update fails
     */
    public void markAsVoted(int electionId, int voterId) throws SQLException {
        String sql = "UPDATE election_voters SET has_voted = TRUE "
                + "WHERE election_id = ? AND voter_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, voterId);
            stmt.executeUpdate();
        }
    }
}
