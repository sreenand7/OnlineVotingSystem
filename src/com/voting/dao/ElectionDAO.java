package com.voting.dao;

import com.voting.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access class for the {@code elections} table.
 *
 * <p>There is no {@code Election} model class in this project, so this DAO
 * exposes a simple nested data carrier, {@link ElectionRecord}, for reading
 * election rows. This DAO performs database operations only — no business
 * rules (e.g. deciding when an election should move from UPCOMING to ACTIVE)
 * belong here.</p>
 */
public class ElectionDAO {

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