package com.campusfix;

import com.campusfix.persistence.FileTicketEventRepository;
import com.campusfix.persistence.FileTicketRepository;
import com.campusfix.persistence.SqliteTicketEventRepository;
import com.campusfix.persistence.SqliteTicketRepository;
import com.campusfix.persistence.TicketEventRepository;
import com.campusfix.persistence.TicketRepository;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.DemoSeeder;
import com.campusfix.service.UserDirectory;
import com.campusfix.service.WeightedPriorityScorer;
import com.campusfix.ui.CampusFixFrame;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.swing.SwingUtilities;

public final class CampusFixApp {
    private CampusFixApp() {
    }

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        Path directory = Paths.get("data");
        boolean forceFile = Arrays.asList(args).contains("--file")
            || "file".equalsIgnoreCase(System.getenv("CAMPUSFIX_STORAGE"));

        TicketRepository tickets;
        TicketEventRepository events;
        if (forceFile) {
            tickets = new FileTicketRepository(directory.resolve("complaints.tsv"));
            events = new FileTicketEventRepository(directory.resolve("events.tsv"));
        } else {
            try {
                Path database = directory.resolve("campusfix.db");
                tickets = new SqliteTicketRepository(database);
                events = new SqliteTicketEventRepository(database);
            } catch (IllegalStateException exception) {
                System.err.println("SQLite unavailable; using local file storage: " + exception.getMessage());
                tickets = new FileTicketRepository(directory.resolve("complaints.tsv"));
                events = new FileTicketEventRepository(directory.resolve("events.tsv"));
            }
        }

        UserDirectory users = new UserDirectory();
        ComplaintService service = new ComplaintService(tickets, events, users, new WeightedPriorityScorer());
        if (!Arrays.asList(args).contains("--empty")) DemoSeeder.seedIfEmpty(service, users);
        SwingUtilities.invokeLater(() -> {
            CampusFixFrame frame = new CampusFixFrame(service, users);
            frame.setVisible(true);
            if (Arrays.asList(args).contains("--demo-admin")) frame.demoLogin("admin");
        });
    }
}
