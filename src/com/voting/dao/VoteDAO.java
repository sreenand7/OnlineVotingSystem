package com.voting.dao;

import com.voting.database.DatabaseConnection;
import com.voting.model.Candidate;
import com.voting.model.Vote;
import com.voting.model.Voter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data-access class for the {@code votes} table.
 *
 * <p>Duplicate voting is prevented at the database level by the
 * {@code UNIQUE (election_id, voter_id)} constraint on {@code votes}, plus
 * the {@code has_voted} flag in {@code election_voters} (managed by
 * {@link VoterDAO}). This DAO does not pre-check for an existing vote before
 * inserting and then work around a failure — it relies on the constraint
 * and simply lets {@link SQLException} (e.g.
 * {@code SQLIntegrityConstraintViolationException}) propagate to the caller
 * if a duplicate insert is attempted.</p>
 *
 * <p>Note: {@link Vote} requires a full {@link Voter} and {@link Candidate}
 * object, not just IDs, so {@link #getVotesForElection(int)} joins against
 * {@code voters} and {@code candidates} to reconstruct them. The
 * reconstructed {@code Voter} objects reflect only stored name/ID — their
 * {@code hasVoted} flag is not set from this query (see {@link VoterDAO} for
 * per-election voting status).</p>
 */
public class VoteDAO {

    /**
     * Records a vote for a specific election.
     *
     * <p>Relies on the database's {@code UNIQUE (election_id, voter_id)}
     * constraint to reject a second vote from the same voter in the same
     * election. Does not manually check for duplicates first.</p>
     *
     * @param electionId  the election this vote belongs to
     * @param voterId     the voter casting the vote
     * @param candidateId the candidate receiving the vote
     * @throws SQLException if the insert fails, including a constraint
     *                       violation on a duplicate vote
     */
    public void saveVote(int electionId, int voterId, int candidateId) throws SQLException {
        String sql = "INSERT INTO votes (election_id, voter_id, candidate_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, voterId);
            stmt.setInt(3, candidateId);
            stmt.executeUpdate();
        }
    }

    /**
     * Returns all votes cast in a specific election, reconstructed as
     * {@link Vote} objects (joining {@code voters} and {@code candidates}
     * for the required {@link Voter}/{@link Candidate} details).
     *
     * @param electionId the election to look up votes for
     * @return list of votes cast in that election (empty list if none)
     * @throws SQLException if the query fails
     */
    public List<Vote> getVotesForElection(int electionId) throws SQLException {
        String sql = "SELECT v.voter_id, vt.name AS voter_name, "
                + "v.candidate_id, c.name AS candidate_name, c.political_party, "
                + "v.vote_time "
                + "FROM votes v "
                + "JOIN voters vt ON vt.voter_id = v.voter_id "
                + "JOIN candidates c ON c.candidate_id = v.candidate_id "
                + "WHERE v.election_id = ?";

        List<Vote> votes = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Voter voter = new Voter(
                            String.valueOf(rs.getInt("voter_id")), rs.getString("voter_name"));
                    Candidate candidate = new Candidate(
                            String.valueOf(rs.getInt("candidate_id")),
                            rs.getString("candidate_name"),
                            rs.getString("political_party"));
                    votes.add(new Vote(voter, candidate, rs.getTimestamp("vote_time").toLocalDateTime()));
                }
            }
        }
        return votes;
    }

    /**
     * Counts votes per candidate for a specific election.
     *
     * @param electionId the election to tally
     * @return map of {@code candidate_id} to vote count (candidates with
     *         zero votes will not appear as keys)
     * @throws SQLException if the query fails
     */
    public Map<Integer, Integer> countVotesByCandidate(int electionId) throws SQLException {
        String sql = "SELECT candidate_id, COUNT(*) AS vote_count FROM votes "
                + "WHERE election_id = ? GROUP BY candidate_id";

        Map<Integer, Integer> tally = new LinkedHashMap<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tally.put(rs.getInt("candidate_id"), rs.getInt("vote_count"));
                }
            }
        }
        return tally;
    }

    /**
     * Checks whether a voter has already cast a vote in a specific election,
     * based on the {@code votes} table itself.
     *
     * @param electionId the election to check
     * @param voterId    the voter to check
     * @return {@code true} if a vote row already exists for this voter in this election
     * @throws SQLException if the query fails
     */
    public boolean hasVoterVoted(int electionId, int voterId) throws SQLException {
        String sql = "SELECT 1 FROM votes WHERE election_id = ? AND voter_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, voterId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
