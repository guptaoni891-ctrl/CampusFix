package com.campusfix.ui;

import com.campusfix.model.User;
import com.campusfix.service.AnalyticsService;
import com.campusfix.service.AnalyticsSnapshot;
import com.campusfix.service.ComplaintFilter;
import com.campusfix.service.TechnicianPerformance;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/** Asynchronous factual technician comparison; unknown durations remain unavailable. */
public final class TechnicianPerformancePanel extends JPanel {
    private final AnalyticsService analyticsService;
    private final User actor;

    public TechnicianPerformancePanel(AnalyticsService analyticsService, User actor) {
        super(new BorderLayout());
        this.analyticsService = analyticsService;
        this.actor = actor;
        setOpaque(false);
        showState("Loading technician performance…");
        load();
    }

    private void load() {
        new SwingWorker<AnalyticsSnapshot, Void>() {
            @Override protected AnalyticsSnapshot doInBackground() {
                return analyticsService.analyze(actor, ComplaintFilter.none());
            }

            @Override protected void done() {
                try {
                    showPerformance(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showState("Technician performance loading was interrupted.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    showState("Unable to load technician performance: "
                        + (cause == null ? exception.getMessage() : cause.getMessage()));
                }
            }
        }.execute();
    }

    private void showPerformance(AnalyticsSnapshot snapshot) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        RoundedPanel notice = new RoundedPanel(new BorderLayout(), 16);
        notice.setBackground(new Color(232, 246, 243));
        notice.setBorder(new EmptyBorder(13, 17, 13, 17));
        JLabel note = Style.label("Metrics use stored assignments and reliable resolution events; no artificial score is calculated.", 11, true);
        note.setForeground(Style.TEAL_DARK);
        notice.add(note, BorderLayout.CENTER);
        notice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.add(notice);
        list.add(Box.createVerticalStrut(13));
        for (TechnicianPerformance performance : snapshot.technicianPerformance()) {
            RoundedPanel card = card(performance);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            list.add(card);
            list.add(Box.createVerticalStrut(11));
        }
        list.add(Box.createVerticalGlue());
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        removeAll();
        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private RoundedPanel card(TechnicianPerformance performance) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(18, 0), 17).withShadow();
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(Style.label(performance.technician().getName(), 15, true));
        JLabel specialty = Style.label(performance.technician().getSpecialty(), 10, false);
        specialty.setForeground(Style.MUTED);
        identity.add(Box.createVerticalStrut(3));
        identity.add(specialty);
        JLabel id = Style.label("Technician ID · " + performance.technician().getId(), 9, false);
        id.setForeground(Style.MUTED);
        identity.add(Box.createVerticalStrut(7));
        identity.add(id);
        card.add(identity, BorderLayout.WEST);

        JPanel metrics = new JPanel(new GridLayout(1, 4, 18, 0));
        metrics.setOpaque(false);
        metrics.add(metric("ACTIVE", String.valueOf(performance.activeTickets()), Style.AMBER));
        metrics.add(metric("RESOLVED", String.valueOf(performance.resolvedTickets()), Style.GREEN));
        metrics.add(metric("AVG. RESOLUTION", duration(performance.averageResolutionDuration()), Style.PURPLE));
        metrics.add(metric("OVERDUE", String.valueOf(performance.overdueActiveTickets()), Style.RED));
        card.add(metrics, BorderLayout.CENTER);
        return card;
    }

    private JPanel metric(String heading, String value, Color color) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = Style.label(heading, 8, true);
        title.setForeground(Style.MUTED);
        JLabel number = Style.label(value, value.length() > 10 ? 13 : 20, true);
        number.setForeground(color.darker());
        panel.add(title);
        panel.add(Box.createVerticalStrut(4));
        panel.add(number);
        return panel;
    }

    private void showState(String text) {
        JPanel state = new JPanel(new GridBagLayout());
        state.setOpaque(false);
        JLabel label = Style.label(text, 13, true);
        label.setForeground(Style.MUTED);
        state.add(label);
        removeAll();
        add(state, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private static String duration(Optional<Duration> duration) {
        if (duration.isEmpty()) return "Not available";
        long hours = duration.get().toHours();
        long minutes = duration.get().minusHours(hours).toMinutes();
        return hours + "h " + minutes + "m";
    }
}
