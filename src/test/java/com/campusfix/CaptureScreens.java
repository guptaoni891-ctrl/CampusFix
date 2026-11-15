package com.campusfix;

import com.campusfix.model.Status;
import com.campusfix.persistence.FileTicketEventRepository;
import com.campusfix.persistence.FileTicketRepository;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.DemoSeeder;
import com.campusfix.service.UserDirectory;
import com.campusfix.service.WeightedPriorityScorer;
import com.campusfix.ui.CampusFixFrame;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

/** Captures live Swing component trees without recording the surrounding desktop. */
public final class CaptureScreens {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "docs/screenshots" : args[0]);
        Files.createDirectories(output);
        Path fixture = Files.createTempDirectory("campusfix-screens-");
        CampusFixFrame frame = null;
        try {
            UserDirectory users = new UserDirectory();
            ComplaintService service = new ComplaintService(
                new FileTicketRepository(fixture.resolve("tickets.tsv")),
                new FileTicketEventRepository(fixture.resolve("events.tsv")),
                users, new WeightedPriorityScorer());
            DemoSeeder.seedIfEmpty(service, users);
            service.addComment(users.login("student", "demo123"), 1001, "The lab connection still drops during class.");
            service.addProgressNote(users.login("tech", "demo123"), 1001, "Checked the access point and isolated the affected switch.");
            frame = edt(() -> {
                CampusFixFrame window = new CampusFixFrame(service, users);
                window.setSize(1280, 800);
                window.setVisible(true);
                return window;
            });
            CampusFixFrame app = frame;
            capture(app, output, "01-login");
            edt(() -> { app.demoLogin("student"); return null; });
            waitFor(() -> tableReady(app));
            capture(app, output, "02-student-dashboard");
            click(app, "Create Complaint");
            capture(app, output, "03-create-complaint");
            edt(() -> { app.demoLogin("admin"); return null; });
            waitFor(() -> tableReady(app));
            capture(app, output, "04-administrator-dashboard");
            click(app, "All Complaints");
            select(app, "#1001");
            JDialog details = modal(app, "Open full ticket", "Complaint #1001");
            waitFor(() -> find(details, JLabel.class, label -> "Activity timeline".equals(label.getText())) != null);
            capture(details, output, "05-complaint-detail");
            captureRegion(details, output, "06-activity-timeline", .56, .12, .43, .84);
            captureRegion(details, output, "08-sla-and-priority", .01, .12, .55, .85);
            close(details);
            edt(() -> { app.demoLogin("tech"); return null; });
            waitFor(() -> tableReady(app));
            capture(app, output, "07-technician-dashboard");
            edt(() -> { app.demoLogin("admin"); return null; });
            click(app, "All Complaints");
            select(app, "#1002");
            JDialog recommendation = modal(app, "Assign technician", "Assign technician");
            capture(recommendation, output, "10-technician-recommendation");
            close(recommendation);
            click(app, "Analytics");
            waitFor(() -> find(app, JLabel.class, label -> "TOTAL COMPLAINTS".equals(label.getText())) != null);
            capture(app, output, "09-analytics");
            click(app, "All Complaints");
            waitFor(() -> tableReady(app));
            JDialog report = modal(app, "Export CSV", "Export report");
            capture(report, output, "11-report-export");
            close(report);
            if (service.get(users.login("admin", "demo123"), 1002).getStatus() != Status.OPEN) {
                throw new AssertionError("Opening the recommendation dialog changed the ticket.");
            }
            System.out.println("PASS: captured eleven live Swing views from isolated demo repositories");
        } finally {
            if (frame != null) close(frame);
            try (var files = Files.walk(fixture)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void select(CampusFixFrame app, String id) throws Exception {
        waitFor(() -> tableReady(app));
        edt(() -> {
            JTable table = find(app, JTable.class, Component::isShowing);
            for (int row = 0; row < table.getRowCount(); row++) {
                if (id.equals(table.getValueAt(row, 0))) {
                    table.setRowSelectionInterval(row, row);
                    table.scrollRectToVisible(table.getCellRect(row, 0, true));
                    return null;
                }
            }
            throw new AssertionError("Missing visible ticket " + id);
        });
    }

    private static boolean tableReady(Container root) {
        return find(root, JTable.class, table -> table.isShowing() && table.getRowCount() > 0) != null;
    }

    private static void click(Container root, String text) throws Exception {
        edt(() -> { button(root, text).doClick(); return null; });
    }

    private static AbstractButton button(Container root, String text) {
        AbstractButton button = find(root, AbstractButton.class, item -> text.equals(item.getText()));
        if (button == null || !button.isEnabled()) throw new AssertionError("Unavailable control " + text);
        return button;
    }

    private static JDialog modal(Window owner, String control, String title) throws Exception {
        AbstractButton button = edt(() -> button(owner, control));
        SwingUtilities.invokeLater(button::doClick);
        waitFor(() -> dialog(title) != null);
        return edt(() -> dialog(title));
    }

    private static JDialog dialog(String title) {
        return Arrays.stream(Window.getWindows()).filter(JDialog.class::isInstance).map(JDialog.class::cast)
            .filter(window -> window.isShowing() && title.equals(window.getTitle())).findFirst().orElse(null);
    }

    private static void capture(Window window, Path output, String name) throws Exception {
        captureRegion(window, output, name, 0, 0, 1, 1);
    }

    private static void captureRegion(Window window, Path output, String name,
            double x, double y, double w, double h) throws Exception {
        BufferedImage image = edt(() -> {
            Container content = ((javax.swing.RootPaneContainer) window).getRootPane();
            BufferedImage shot = new BufferedImage(content.getWidth(), content.getHeight(), BufferedImage.TYPE_INT_RGB);
            var graphics = shot.createGraphics();
            try { content.printAll(graphics); } finally { graphics.dispose(); }
            return shot;
        });
        ImageIO.write(image.getSubimage((int)(x * image.getWidth()), (int)(y * image.getHeight()),
            (int)(w * image.getWidth()), (int)(h * image.getHeight())),
            "png", output.resolve(name + ".png").toFile());
    }

    private static void close(Window window) throws Exception {
        edt(() -> { window.dispose(); return null; });
    }

    private static void waitFor(Callable<Boolean> predicate) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (edt(predicate)) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for Swing content");
    }

    private static <T extends Component> T find(Component root, Class<T> type,
            java.util.function.Predicate<T> predicate) {
        if (type.isInstance(root) && predicate.test(type.cast(root))) return type.cast(root);
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                T match = find(child, type, predicate);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static <T> T edt(Callable<T> operation) throws Exception {
        FutureTask<T> task = new FutureTask<>(operation);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
