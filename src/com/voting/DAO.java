package com.voting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class CandidateDAO {

    /**
     * Registers a new candidate for a specific election.
     *
     * @param electionId     the election this candidate is standing in
     * @param name           candidate's full name
     * @param politicalParty candidate's political party
     * @return the generated {@code candidate_id}
     * @throws SQLException if the insert fails
     */
    public int addCandidate(int electionId, String name, String politicalParty) throws SQLException {
        String sql = "INSERT INTO candidates (election_id, name, political_party) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, electionId);
            stmt.setString(2, name);
            stmt.setString(3, politicalParty);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Candidate insert did not return a generated ID.");
            }
        }
    }

    /**
     * Finds a candidate by ID, regardless of which election they belong to.
     *
     * @param candidateId the candidate ID to look up
     * @return a {@link Candidate} built from the stored row, or {@code null} if not found
     * @throws SQLException if the query fails
     */
    public Candidate findByCandidateId(int candidateId) throws SQLException {
        String sql = "SELECT candidate_id, name, political_party FROM candidates WHERE candidate_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, candidateId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Returns all candidates standing in a specific election.
     *
     * @param electionId the election to look up candidates for
     * @return list of candidates in that election (empty list if none)
     * @throws SQLException if the query fails
     */
    public List<Candidate> getCandidatesForElection(int electionId) throws SQLException {
        String sql = "SELECT candidate_id, name, political_party FROM candidates WHERE election_id = ?";

        List<Candidate> candidates = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    candidates.add(mapRow(rs));
                }
            }
        }
        return candidates;
    }

    /**
     * Checks whether a candidate exists within a specific election.
     *
     * @param electionId  the election to check within
     * @param candidateId the candidate ID to check
     * @return {@code true} if that candidate exists in that election
     * @throws SQLException if the query fails
     */
    public boolean candidateExists(int electionId, int candidateId) throws SQLException {
        String sql = "SELECT 1 FROM candidates WHERE election_id = ? AND candidate_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);
            stmt.setInt(2, candidateId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Candidate mapRow(ResultSet rs) throws SQLException {
        return new Candidate(
                String.valueOf(rs.getInt("candidate_id")),
                rs.getString("name"),
                rs.getString("political_party")
        );
    }
}

class ElectionDAO {

    /**
     * Allowed values of the {@code status} column, mirroring the database
     * ENUM('UPCOMING', 'ACTIVE', 'COMPLETED').
     */
    public enum Status {
        UPCOMING, ACTIVE, COMPLETED
    }

    /**
     * Simple, immutable read-only carrier for an {@code elections} row.
     * Not a model class in the OOP sense — just a plain data holder returned
     * by query methods, since the project has no dedicated Election model.
     */
    public static final class ElectionRecord {
        public final int electionId;
        public final String name;
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;
        public final String status;
        public final Integer parentElectionId; // null if this is not a re-election

        public ElectionRecord(int electionId, String name, LocalDateTime startTime,
                               LocalDateTime endTime, String status, Integer parentElectionId) {
            this.electionId = electionId;
            this.name = name;
            this.startTime = startTime;
            this.endTime = endTime;
            this.status = status;
            this.parentElectionId = parentElectionId;
        }

        @Override
        public String toString() {
            return String.format(
                    "ElectionRecord[id=%d, name='%s', start=%s, end=%s, status=%s, parentId=%s]",
                    electionId, name, startTime, endTime, status, parentElectionId);
        }
    }

    /**
     * Creates a new election with status UPCOMING (the table default).
     *
     * @param name      election name
     * @param startTime scheduled start time
     * @param endTime   scheduled end time
     * @return the generated {@code election_id}
     * @throws SQLException if the insert fails
     */
    public int createElection(String name, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {
        return createElection(name, startTime, endTime, null);
    }

    /**
     * Creates a new election, optionally linked to a prior election via
     * {@code parent_election_id} (used for re-elections). Status defaults
     * to UPCOMING.
     *
     * @param name             election name
     * @param startTime        scheduled start time
     * @param endTime          scheduled end time
     * @param parentElectionId the original election's ID if this is a
     *                         re-election, or {@code null} otherwise
     * @return the generated {@code election_id}
     * @throws SQLException if the insert fails
     */
    public int createElection(String name, LocalDateTime startTime, LocalDateTime endTime,
                               Integer parentElectionId) throws SQLException {
        String sql = "INSERT INTO elections (name, start_time, end_time, parent_election_id) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setTimestamp(2, Timestamp.valueOf(startTime));
            stmt.setTimestamp(3, Timestamp.valueOf(endTime));
            if (parentElectionId == null) {
                stmt.setNull(4, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(4, parentElectionId);
            }

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new SQLException("Election insert did not return a generated ID.");
            }
        }
    }

    /**
     * Finds the most recently created election with the given name.
     * (Names are not guaranteed unique, e.g. after a re-election, so this
     * returns the latest match by election_id.)
     *
     * @param name election name to search for
     * @return the matching {@link ElectionRecord}, or {@code null} if none found
     * @throws SQLException if the query fails
     */
    public ElectionRecord findByName(String name) throws SQLException {
        String sql = "SELECT election_id, name, start_time, end_time, status, parent_election_id "
                + "FROM elections WHERE name = ? ORDER BY election_id DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Finds an election by its primary key.
     *
     * @param electionId the election ID to look up
     * @return the matching {@link ElectionRecord}, or {@code null} if not found
     * @throws SQLException if the query fails
     */
    public ElectionRecord findById(int electionId) throws SQLException {
        String sql = "SELECT election_id, name, start_time, end_time, status, parent_election_id "
                + "FROM elections WHERE election_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, electionId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * Returns all elections regardless of status, most recent first.
     *
     * @return list of all elections (empty list if none)
     * @throws SQLException if the query fails
     */
    public List<ElectionRecord> getAllElections() throws SQLException {
        String sql = "SELECT election_id, name, start_time, end_time, status, parent_election_id "
                + "FROM elections ORDER BY election_id DESC";

        List<ElectionRecord> results = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    /**
     * Returns all elections whose status is COMPLETED, most recent first.
     *
     * @return list of completed elections (empty list if none)
     * @throws SQLException if the query fails
     */
    public List<ElectionRecord> getCompletedElections() throws SQLException {
        String sql = "SELECT election_id, name, start_time, end_time, status, parent_election_id "
                + "FROM elections WHERE status = 'COMPLETED' ORDER BY election_id DESC";

        List<ElectionRecord> results = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    /**
     * Fixes the real start/end time of an election and flips it to ACTIVE
     * in a single update. Used when an operator explicitly starts voting on
     * a previously UPCOMING election (the row is created earlier with
     * placeholder times, since {@code start_time}/{@code end_time} are
     * NOT NULL but the real voting window is only known once voting begins).
     *
     * @param electionId the election to start
     * @param startTime  the real voting-window start (now)
     * @param endTime    the real voting-window end (now + duration)
     * @throws SQLException if the update fails
     */
    public void startElection(int electionId, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {
        String sql = "UPDATE elections SET start_time = ?, end_time = ?, status = 'ACTIVE' "
                + "WHERE election_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, startTime);
            stmt.setObject(2, endTime);
            stmt.setInt(3, electionId);
            stmt.executeUpdate();
        }
    }

    /**
     * Updates the status of an election (e.g. UPCOMING → ACTIVE → COMPLETED).
     *
     * @param electionId the election to update
     * @param status     the new status
     * @throws SQLException if the update fails
     */
    public void updateStatus(int electionId, Status status) throws SQLException {
        String sql = "UPDATE elections SET status = ? WHERE election_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, electionId);
            stmt.executeUpdate();
        }
    }

    private ElectionRecord mapRow(ResultSet rs) throws SQLException {
        int parentId = rs.getInt("parent_election_id");
        Integer parentElectionId = rs.wasNull() ? null : parentId;

        return new ElectionRecord(
                rs.getInt("election_id"),
                rs.getString("name"),
                rs.getObject("start_time", LocalDateTime.class),
                rs.getObject("end_time", LocalDateTime.class),
                rs.getString("status"),
                parentElectionId
        );
    }
}

class VoteDAO {

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

class VoterDAO {

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

