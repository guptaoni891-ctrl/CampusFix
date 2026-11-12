package com.campusfix.persistence;

import com.campusfix.model.TicketEvent;
import com.campusfix.model.TicketEventType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Separate versioned event file that leaves legacy complaint records unchanged. */
public final class FileTicketEventRepository implements TicketEventRepository {
    private static final int FIELD_COUNT = 7;
    private static final String VERSION = "v1";
    private final Path path;
    private final Map<Long, TicketEvent> events = new LinkedHashMap<>();

    public FileTicketEventRepository(Path path) {
        this.path = path;
        load();
    }

    private void load() {
        if (!Files.exists(path)) return;
        int lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                try {
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != FIELD_COUNT) {
                        throw new IllegalArgumentException("expected " + FIELD_COUNT + " fields but found " + fields.length);
                    }
                    for (int index = 0; index < fields.length; index++) fields[index] = decode(fields[index]);
                    if (!VERSION.equals(fields[6])) {
                        throw new IllegalArgumentException("unsupported event record version " + fields[6]);
                    }
                    TicketEvent event = new TicketEvent(
                        Long.parseLong(fields[0]), Integer.parseInt(fields[1]), fields[2],
                        TicketEventType.valueOf(fields[3]), LocalDateTime.parse(fields[4]),
                        fields[5].isEmpty() ? null : fields[5]);
                    if (events.putIfAbsent(event.getEventId(), event) != null) {
                        throw new IllegalArgumentException("duplicate event ID " + event.getEventId());
                    }
                } catch (RuntimeException exception) {
                    throw new IOException("Malformed ticket event at line " + lineNumber + ": " + exception.getMessage(), exception);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load ticket event file " + path + ". " + exception.getMessage(), exception);
        }
    }

    @Override
    public synchronized List<TicketEvent> findByComplaintId(int complaintId) {
        return events.values().stream()
            .filter(event -> event.getComplaintId() == complaintId)
            .sorted(Comparator.comparing(TicketEvent::getTimestamp).thenComparingLong(TicketEvent::getEventId))
            .toList();
    }

    @Override
    public synchronized long nextId() {
        return events.keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1;
    }

    @Override
    public synchronized void save(TicketEvent event) {
        if (events.containsKey(event.getEventId())) {
            throw new IllegalStateException("Duplicate ticket event ID: " + event.getEventId());
        }
        Path temporary = null;
        try {
            Path directory = path.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            Map<Long, TicketEvent> replacement = new LinkedHashMap<>(events);
            replacement.put(event.getEventId(), event);
            temporary = Files.createTempFile(directory, "campusfix-events-", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                for (TicketEvent stored : replacement.values()) {
                    String[] fields = {
                        String.valueOf(stored.getEventId()), String.valueOf(stored.getComplaintId()),
                        stored.getActorId(), stored.getEventType().name(), stored.getTimestamp().toString(),
                        stored.getNote() == null ? "" : stored.getNote(), VERSION
                    };
                    for (int index = 0; index < fields.length; index++) fields[index] = encode(fields[index]);
                    writer.write(String.join("\t", fields));
                    writer.newLine();
                }
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
            events.clear();
            events.putAll(replacement);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save ticket activity to " + path, exception);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
