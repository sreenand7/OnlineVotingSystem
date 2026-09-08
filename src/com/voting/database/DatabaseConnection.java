package com.voting.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Provides JDBC connections to the {@code online_voting} MySQL database.
 *
 * <p>This class has no dependency on any model, manager, or exception class
 * in the application — it is purely infrastructure. DAO classes (added in a
 * later step) will call {@link #getConnection()} to obtain a connection.</p>
 *
 * <p>Connection details (host, port, database name, credentials) are kept
 * as constants here for now. In a later step these can be externalized to
 * a properties file if desired — not done yet, per current scope.</p>
 */
public final class DatabaseConnection {

    private static final String HOST     = "127.0.0.1";
    private static final String PORT     = "3306";
    private static final String DATABASE = "online_voting";

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                    + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static final String USER     = "root";
    private static final String PASSWORD = ""; // TODO: replace with your actual MySQL root password

    // Prevent instantiation — this class only exposes static behavior.
    private DatabaseConnection() {
    }

    /**
     * Opens a new JDBC connection to the {@code online_voting} database.
     *
     * <p>Callers are responsible for closing the returned {@link Connection}
     * (ideally via try-with-resources) once they are done with it.</p>
     *
     * @return an open {@link Connection} to the database
     * @throws SQLException if the connection cannot be established
     *                       (e.g. MySQL is not running, wrong credentials,
     *                       or the database does not exist)
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}