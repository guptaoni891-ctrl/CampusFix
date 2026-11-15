package com.campusfix.ui;

import com.campusfix.model.Category;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import com.campusfix.service.AnalyticsService;
import com.campusfix.service.AnalyticsSnapshot;
import com.campusfix.service.ComplaintFilter;
import com.campusfix.service.ReportService;
import com.campusfix.service.ReportType;
import com.campusfix.service.TechnicianPerformance;
import com.campusfix.service.UserDirectory;
import com.campusfix.model.User;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/** Filterable, asynchronous administrator analytics dashboard with Java2D charts. */
public final class AnalyticsDashboardPanel extends JPanel {
    private static final String LOADING = "loading";
    private static final String CONTENT = "content";
    private static final String ERROR = "error";
    private final AnalyticsService analyticsService;
    private final ReportService reportService;
    private final User actor;
    private final ComplaintFilterBar filters;
    private final JPanel cards = new JPanel(new java.awt.CardLayout());
    private final JLabel loadingMessage = Style.label("Calculating analytics…", 13, true);
    private final JLabel errorMessage = Style.label("", 12, false);
    private final ModernButton exportSla = Style.secondary("Export SLA CSV");
    private final ModernButton exportTechnicians = Style.secondary("Export technician CSV");
    private AnalyticsSnapshot currentSnapshot;
    private int loadGeneration;
    private boolean exportRunning;

    public AnalyticsDashboardPanel(AnalyticsService analyticsService, ReportService reportService,
                                   UserDirectory directory, User actor) {
        super(new BorderLayout(0, 14));
        this.analyticsService = analyticsService;
        this.reportService = reportService;
        this.actor = actor;
        setOpaque(false);
        filters = new ComplaintFilterBar(directory);
        filters.setListener((filter, sort) -> loadAsync(filter));
        add(filterCard(), BorderLayout.NORTH);
        cards.setOpaque(false);
        cards.add(statePanel(loadingMessage), LOADING);
        cards.add(statePanel(errorMessage), ERROR);
        add(cards, BorderLayout.CENTER);
        loadAsync(ComplaintFilter.none());
    }

    public ComplaintFilter currentFilter() {
        return filters.currentFilter();
    }

    private JPanel filterCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(12, 10), 17).withShadow();
        card.setBorder(new EmptyBorder(15, 17, 15, 17));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(Style.label("Analytics filters", 15, true));
        JLabel hint = Style.label("Every KPI and chart below reflects the selected filters.", 10, false);
        hint.setForeground(Style.MUTED);
        copy.add(hint);
        heading.add(copy, BorderLayout.WEST);
        JPanel exports = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        exports.setOpaque(false);
        exportSla.addActionListener(event -> export(ReportType.SLA_SUMMARY, "campusfix_sla_summary.csv", exportSla));
        exportTechnicians.addActionListener(event -> export(
            ReportType.TECHNICIAN_SUMMARY, "campusfix_technician_summary.csv", exportTechnicians));
        exports.add(exportSla);
        exports.add(exportTechnicians);
        heading.add(exports, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        card.add(filters, BorderLayout.CENTER);
        return card;
    }

    private void loadAsync(ComplaintFilter filter) {
        int generation = ++loadGeneration;
        filters.setLoading(true);
        ((java.awt.CardLayout) cards.getLayout()).show(cards, LOADING);
        new SwingWorker<AnalyticsSnapshot, Void>() {
            @Override protected AnalyticsSnapshot doInBackground() {
                return analyticsService.analyze(actor, filter);
            }

            @Override protected void done() {
                if (generation != loadGeneration) return;
                try {
                    currentSnapshot = get();
                    showSnapshot(currentSnapshot);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Analytics loading was interrupted.");
                } catch (ExecutionException exception) {
                    showError(message(exception.getCause()));
                } finally {
                    filters.setLoading(false);
                }
            }
        }.execute();
    }

    private void showSnapshot(AnalyticsSnapshot snapshot) {
        Component existing = findContentCard();
        if (existing != null) cards.remove(existing);
        cards.add(scrollable(dashboard(snapshot)), CONTENT);
        ((java.awt.CardLayout) cards.getLayout()).show(cards, CONTENT);
        cards.revalidate();
        cards.repaint();
    }

    private Component findContentCard() {
        for (Component component : cards.getComponents()) {
            if (component instanceof JScrollPane) return component;
        }
        return null;
    }

    private JPanel dashboard(AnalyticsSnapshot snapshot) {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel kpis = new JPanel(new GridLayout(2, 3, 12, 12));
        kpis.setOpaque(false);
        kpis.add(new SummaryCard("Total complaints", String.valueOf(snapshot.totalComplaints()),
            snapshot.filter().isEmpty() ? "All stored complaints" : "Filtered selection", Style.BLUE));
        kpis.add(new SummaryCard("Resolution rate", percent(snapshot.resolutionRate()),
            "Resolved / total", Style.GREEN));
        kpis.add(new SummaryCard("Overdue", String.valueOf(snapshot.overdueUnresolved()),
            "Unresolved past deadline", Style.RED));
        kpis.add(new SummaryCard("Due soon", String.valueOf(snapshot.dueSoonUnresolved()),
            "Inside warning window", Style.AMBER));
        kpis.add(new SummaryCard("Avg. resolution", duration(snapshot.averageResolutionDuration()),
            "Reliable timestamps only", Style.PURPLE));
        kpis.add(new SummaryCard("SLA compliance", percent(snapshot.slaComplianceRate()),
            snapshot.knownSlaOutcomes() + " known resolved outcome(s)", Style.TEAL));
        kpis.setMaximumSize(new Dimension(Integer.MAX_VALUE, 246));
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(kpis);
        content.add(Box.createVerticalStrut(14));

        JPanel distributions = new JPanel(new GridLayout(1, 3, 13, 0));
        distributions.setOpaque(false);
        distributions.add(chartCard("Status distribution", "Current workflow state",
            new DonutChartPanel(statusValues(snapshot), Style.AMBER, Style.BLUE, Style.PURPLE, Style.GREEN)));
        distributions.add(chartCard("Maintenance categories", "Complaints by issue type",
            new BarChartPanel(categoryValues(snapshot), Style.TEAL)));
        distributions.add(chartCard("Severity distribution", "Reported operational impact",
            new BarChartPanel(severityValues(snapshot), Style.PURPLE)));
        distributions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 285));
        distributions.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(distributions);
        content.add(Box.createVerticalStrut(14));

        TrendChartPanel trend = new TrendChartPanel(snapshot.creationTrend());
        RoundedPanel trendCard = chartCard("Complaint creation trend", "Daily complaint volume", trend);
        trendCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));
        trendCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(trendCard);
        content.add(Box.createVerticalStrut(14));

        JPanel operations = new JPanel(new GridLayout(1, 2, 13, 0));
        operations.setOpaque(false);
        operations.add(chartCard("Technician active workload", "Currently assigned unresolved tickets",
            new BarChartPanel(workloadValues(snapshot), Style.BLUE)));
        operations.add(chartCard("Resolved SLA outcomes",
            "Unknown legacy outcomes are excluded",
            new DonutChartPanel(slaValues(snapshot), Style.GREEN, Style.RED)));
        operations.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        operations.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(operations);
        content.add(Box.createVerticalStrut(10));
        return content;
    }

    private RoundedPanel chartCard(String title, String subtitle, JPanel chart) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 8), 17).withShadow();
        card.setBorder(new EmptyBorder(16, 17, 17, 17));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Style.label(title, 14, true));
        JLabel hint = Style.label(subtitle, 10, false);
        hint.setForeground(Style.MUTED);
        heading.add(hint);
        card.add(heading, BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(250, 190));
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane scrollable(Component content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel statePanel(JLabel label) {
        JPanel state = new JPanel(new GridBagLayout());
        state.setOpaque(false);
        label.setForeground(Style.MUTED);
        state.add(label);
        return state;
    }

    private void showError(String message) {
        errorMessage.setText("Unable to calculate analytics: " + message);
        ((java.awt.CardLayout) cards.getLayout()).show(cards, ERROR);
    }

    private void export(ReportType type, String filename, ModernButton button) {
        if (exportRunning || currentSnapshot == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export " + type);
        chooser.setSelectedFile(new java.io.File(filename));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path file = chooser.getSelectedFile().toPath();
        exportRunning = true;
        exportSla.setEnabled(false);
        exportTechnicians.setEnabled(false);
        String original = button.getText();
        button.setText("Exporting…");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws IOException {
                reportService.export(actor, file, type, filters.currentFilter());
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    JOptionPane.showMessageDialog(AnalyticsDashboardPanel.this,
                        "Report exported to " + file.toAbsolutePath(), "CampusFix",
                        JOptionPane.INFORMATION_MESSAGE);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showExportError("Export was interrupted.");
                } catch (ExecutionException exception) {
                    showExportError(message(exception.getCause()));
                } finally {
                    exportRunning = false;
                    exportSla.setEnabled(true);
                    exportTechnicians.setEnabled(true);
                    button.setText(original);
                }
            }
        }.execute();
    }

    private void showExportError(String message) {
        JOptionPane.showMessageDialog(this, message, "CampusFix export",
            JOptionPane.WARNING_MESSAGE);
    }

    private static Map<String, Long> statusValues(AnalyticsSnapshot snapshot) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (Status value : Status.values()) values.put(friendly(value.name()), snapshot.count(value));
        return values;
    }

    private static Map<String, Long> categoryValues(AnalyticsSnapshot snapshot) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (Category value : Category.values()) {
            values.put(friendly(value.name()), snapshot.categoryCounts().getOrDefault(value, 0L));
        }
        return values;
    }

    private static Map<String, Long> severityValues(AnalyticsSnapshot snapshot) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (Severity value : Severity.values()) {
            values.put(friendly(value.name()), snapshot.severityCounts().getOrDefault(value, 0L));
        }
        return values;
    }

    private static Map<String, Long> workloadValues(AnalyticsSnapshot snapshot) {
        Map<String, Long> values = new LinkedHashMap<>();
        for (TechnicianPerformance performance : snapshot.technicianPerformance()) {
            values.put(performance.technician().getName(), performance.activeTickets());
        }
        return values;
    }

    private static Map<String, Long> slaValues(AnalyticsSnapshot snapshot) {
        Map<String, Long> values = new LinkedHashMap<>();
        values.put("Within SLA", snapshot.slaMet());
        values.put("After SLA", snapshot.slaBreached());
        return values;
    }

    private static String percent(OptionalDouble value) {
        return value.isPresent() ? String.format(Locale.ROOT, "%.1f%%", value.getAsDouble()) : "Not available";
    }

    private static String duration(Optional<Duration> value) {
        if (value.isEmpty()) return "Not available";
        long hours = value.get().toHours();
        long minutes = value.get().minusHours(hours).toMinutes();
        return hours + "h " + minutes + "m";
    }

    private static String friendly(String value) {
        String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String message(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
            ? "The operation could not be completed." : throwable.getMessage();
    }
}
