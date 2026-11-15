package com.campusfix.persistence;

import com.campusfix.model.TicketEvent;
import com.campusfix.model.TicketEventType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Additive SQLite persistence for complaint activity. */
public final class SqliteTicketEventRepository implements TicketEventRepository {
    private final String jdbcUrl;

    public SqliteTicketEventRepository(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Class.forName("org.sqlite.JDBC");
            jdbcUrl = "jdbc:sqlite:" + file.toAbsolutePath();
            try (Connection connection = DriverManager.getConnection(jdbcUrl);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS complaint_events (
                      event_id INTEGER PRIMARY KEY,
                      complaint_id INTEGER NOT NULL,
                      actor_id TEXT NOT NULL,
                      event_type TEXT NOT NULL,
                      event_timestamp TEXT NOT NULL,
                      note TEXT,
                      FOREIGN KEY (complaint_id) REFERENCES complaints(id))
                    """);
                statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_complaint_events_ticket_time
                    ON complaint_events(complaint_id, event_timestamp, event_id)
                    """);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("SQLite event storage unavailable: " + exception.getMessage(), exception);
        }
    }

    @Override
    public synchronized List<TicketEvent> findByComplaintId(int complaintId) {
        String sql = """
            SELECT event_id, complaint_id, actor_id, event_type, event_timestamp, note
            FROM complaint_events
            WHERE complaint_id = ?
            ORDER BY event_timestamp ASC, event_id ASC
            """;
        List<TicketEvent> result = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, complaintId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(new TicketEvent(
                        rows.getLong("event_id"), rows.getInt("complaint_id"), rows.getString("actor_id"),
                        TicketEventType.valueOf(rows.getString("event_type")),
                        LocalDateTime.parse(rows.getString("event_timestamp")), rows.getString("note")));
                }
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new IllegalStateException("Database ticket-event read error for complaint #" + complaintId, exception);
        }
        return result;
    }

    @Override
    public synchronized long nextId() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT COALESCE(MAX(event_id), 0) + 1 FROM complaint_events")) {
            return row.next() ? row.getLong(1) : 1L;
        } catch (SQLException exception) {
            throw new IllegalStateException("Database ticket-event ID error", exception);
        }
    }

    @Override
    public synchronized void save(TicketEvent event) {
        String sql = """
            INSERT INTO complaint_events
              (event_id, complaint_id, actor_id, event_type, event_timestamp, note)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, event.getEventId());
            statement.setInt(2, event.getComplaintId());
            statement.setString(3, event.getActorId());
            statement.setString(4, event.getEventType().name());
            statement.setString(5, event.getTimestamp().toString());
            statement.setString(6, event.getNote());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Database ticket-event write error", exception);
        }
    }
}
