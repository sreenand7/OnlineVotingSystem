package com.voting;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class CandidateDAO {

    public int addCandidate(int electionId, String name, String politicalParty) throws SQLException {
        String sql = "INSERT INTO candidates (election_id, name, political_party) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, electionId);
            stmt.setString(2, name);
            stmt.setString(3, politicalParty);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Candidate insert did not return a generated ID.");
            }
        }
    }

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

    public List<Candidate> getCandidatesForElection(int electionId) throws SQLException {
        String sql = "SELECT candidate_id, name, political_party FROM candidates WHERE election_id = ?";
        List<Candidate> candidates = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, electionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) candidates.add(mapRow(rs));
            }
        }
        return candidates;
    }

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
                rs.getString("political_party"));
    }
}

class ElectionDAO {

    public enum Status { UPCOMING, ACTIVE, COMPLETED }

    public static final class ElectionRecord {
        public final int electionId;
        public final String name;
        public final LocalDateTime startTime;
        public final LocalDateTime endTime;
        public final String status;
        public final Integer parentElectionId;

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

    public int createElection(String name, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {
        return createElection(name, startTime, endTime, null);
    }

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
                stmt.setNull(4, Types.INTEGER);
            } else {
                stmt.setInt(4, parentElectionId);
            }
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
                throw new SQLException("Election insert did not return a generated ID.");
            }
        }
    }

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

    public List<ElectionRecord> getAllElections() throws SQLException {
        String sql = "SELECT election_id, name, start_time, end_time, status, parent_election_id "
                + "FROM elections ORDER BY election_id DESC";
        List<ElectionRecord> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) results.add(mapRow(rs));
        }
        return results;
    }

    public List<ElectionRecord> getCompletedElections() throws SQLException {
        String sql = "SELECT election_id, name, start_time, end_time, status, parent_election_id "
                + "FROM elections WHERE status = 'COMPLETED' ORDER BY election_id DESC";
        List<ElectionRecord> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) results.add(mapRow(rs));
        }
        return results;
    }

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
                parentElectionId);
    }
}

class VoteDAO {

    // Relies on UNIQUE (election_id, voter_id) constraint for duplicate prevention
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

    public void registerVoter(int voterId, String name) throws SQLException {
        String sql = "INSERT INTO voters (voter_id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, voterId);
            stmt.setString(2, name);
            stmt.executeUpdate();
        }
    }

    public Voter findByVoterId(int voterId) throws SQLException {
        String sql = "SELECT voter_id, name FROM voters WHERE voter_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, voterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return new Voter(String.valueOf(rs.getInt("voter_id")), rs.getString("name"));
                return null;
            }
        }
    }

    public List<Voter> getAllVoters() throws SQLException {
        String sql = "SELECT voter_id, name FROM voters";
        List<Voter> voters = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next())
                voters.add(new Voter(String.valueOf(rs.getInt("voter_id")), rs.getString("name")));
        }
        return voters;
    }

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
                while (rs.next())
                    voters.add(new Voter(String.valueOf(rs.getInt("voter_id")), rs.getString("name")));
            }
        }
        return voters;
    }

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

    public boolean hasVoted(int electionId, int voterId) throws SQLException {
        String sql = "SELECT has_voted FROM election_voters WHERE election_id = ? AND voter_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, electionId);
            stmt.setInt(2, voterId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean("has_voted");
                throw new SQLException(
                        "Voter " + voterId + " is not enrolled in election " + electionId + ".");
            }
        }
    }

    public boolean isVoterInElection(int electionId, int voterId) throws SQLException {
        String sql = "SELECT 1 FROM election_voters WHERE election_id = ? AND voter_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, electionId);
            stmt.setInt(2, voterId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

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
