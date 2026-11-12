package com.campusfix.ui;

import com.campusfix.model.Complaint;
import com.campusfix.model.Role;
import com.campusfix.model.Status;
import com.campusfix.model.TicketEvent;
import com.campusfix.model.User;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.UserDirectory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/** Asynchronous, role-aware complaint details and activity timeline dialog. */
public final class ComplaintDetailDialog extends JDialog {
    private final ComplaintService service;
    private final UserDirectory directory;
    private final User actor;
    private final int complaintId;
    private final Runnable onChanged;
    private final DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private Complaint complaint;

    public static void open(Window owner, ComplaintService service, UserDirectory directory,
                            User actor, int complaintId, Runnable onChanged) {
        ComplaintDetailDialog dialog = new ComplaintDetailDialog(
            owner, service, directory, actor, complaintId, onChanged);
        dialog.setVisible(true);
    }

    private ComplaintDetailDialog(Window owner, ComplaintService service, UserDirectory directory,
                                  User actor, int complaintId, Runnable onChanged) {
        super(owner, "Complaint #" + complaintId, ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.directory = directory;
        this.actor = actor;
        this.complaintId = complaintId;
        this.onChanged = onChanged;
        setSize(980, 720);
        setMinimumSize(new Dimension(820, 620));
        setLocationRelativeTo(owner);
        loadDetails();
    }

    private void loadDetails() {
        setContentPane(loadingPanel("Loading ticket activity…"));
        revalidate();
        repaint();
        new SwingWorker<DetailData, Void>() {
            @Override protected DetailData doInBackground() {
                return new DetailData(service.get(actor, complaintId), service.eventsFor(actor, complaintId));
            }

            @Override protected void done() {
                try {
                    DetailData data = get();
                    complaint = data.complaint();
                    setContentPane(content(data));
                    revalidate();
                    repaint();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showLoadFailure("Loading was interrupted.");
                } catch (ExecutionException exception) {
                    showLoadFailure(message(exception.getCause()));
                }
            }
        }.execute();
    }

    private JPanel content(DetailData data) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Style.BG);
        root.add(header(data.complaint()), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            ticketInformation(data.complaint()), activityPanel(data));
        split.setResizeWeight(.43);
        split.setDividerSize(8);
        split.setBorder(new EmptyBorder(16, 18, 18, 18));
        split.setBackground(Style.BG);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    private JPanel header(Complaint ticket) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(Style.NAVY);
        header.setBorder(new EmptyBorder(18, 22, 18, 22));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel eyebrow = Style.label("TICKET #" + ticket.getId(), 10, true);
        eyebrow.setForeground(new Color(110, 222, 196));
        JLabel title = Style.label(ticket.getTitle(), 20, true);
        title.setForeground(Style.WHITE);
        copy.add(eyebrow);
        copy.add(Box.createVerticalStrut(4));
        copy.add(title);
        header.add(copy, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        actions.setOpaque(false);
        actions.add(new StatusBadge(ticket.getStatus()));
        Status next = nextStatus(ticket.getStatus());
        if (actor.getRole() != Role.STUDENT && next != null) {
            ModernButton advance = Style.button(next == Status.RESOLVED ? "Resolve ticket" : "Start progress");
            advance.addActionListener(event -> advanceStatus(next));
            actions.add(advance);
        }
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JScrollPane ticketInformation(Complaint ticket) {
        JPanel content = new JPanel();
        content.setBackground(Style.BG);
        content.setBorder(new EmptyBorder(0, 0, 0, 7));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        RoundedPanel overview = card();
        overview.setLayout(new BoxLayout(overview, BoxLayout.Y_AXIS));
        JLabel sectionTitle = Style.label("Complaint overview", 15, true);
        sectionTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        overview.add(sectionTitle);
        overview.add(Box.createVerticalStrut(12));
        JTextArea description = readOnlyText(ticket.getDescription());
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        overview.add(description);
        overview.add(Box.createVerticalStrut(16));
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        badgeRow.add(new StatusBadge(ticket.getSeverity()));
        JLabel priority = Style.label("Priority " + service.score(ticket) + "/100", 11, true);
        priority.setForeground(service.score(ticket) >= 75 ? Style.RED : Style.TEAL_DARK);
        badgeRow.add(priority);
        overview.add(badgeRow);
        content.add(overview);
        content.add(Box.createVerticalStrut(12));

        RoundedPanel workflow = card();
        workflow.setLayout(new BorderLayout(0, 9));
        workflow.add(Style.label("Status progression", 15, true), BorderLayout.NORTH);
        workflow.add(new StatusProgressPanel(ticket.getStatus()), BorderLayout.CENTER);
        content.add(workflow);
        content.add(Box.createVerticalStrut(12));

        RoundedPanel details = card();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel informationTitle = Style.label("Ticket information", 15, true);
        informationTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(informationTitle);
        details.add(Box.createVerticalStrut(12));
        User reporter = directory.byId(ticket.getStudentId());
        User technician = directory.byId(ticket.getTechnicianId());
        details.add(detailGrid(
            "Category", friendly(ticket.getCategory().name()),
            "Location", ticket.getLocation(),
            "Reporter", reporter == null ? ticket.getStudentId() : reporter.getName(),
            "Technician", technician == null ? "Not assigned" : technician.getName(),
            "Created", date.format(ticket.getCreatedAt()),
            "Last updated", date.format(ticket.getUpdatedAt())));
        content.add(details);
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        return scroll;
    }

    private RoundedPanel activityPanel(DetailData data) {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 14), 18).withShadow();
        panel.setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Style.label("Activity timeline", 17, true));
        JLabel hint = Style.label(data.events().size() + " recorded event(s) · oldest first", 11, false);
        hint.setForeground(Style.MUTED);
        heading.add(Box.createVerticalStrut(3));
        heading.add(hint);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(timeline(data.events()), BorderLayout.CENTER);
        panel.add(composer(data.complaint()), BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane timeline(List<TicketEvent> events) {
        JPanel list = new JPanel();
        list.setBackground(Style.WHITE);
        list.setBorder(new EmptyBorder(3, 2, 3, 4));
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        if (events.isEmpty()) {
            JPanel legacy = new JPanel();
            legacy.setOpaque(false);
            legacy.setLayout(new BoxLayout(legacy, BoxLayout.Y_AXIS));
            legacy.setBorder(new EmptyBorder(45, 28, 45, 28));
            EmptyTimelineIcon icon = new EmptyTimelineIcon();
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel title = Style.label("No recorded activity", 14, true);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel note = Style.label("This may be a legacy complaint created before activity tracking.", 11, false);
            note.setForeground(Style.MUTED);
            note.setAlignmentX(Component.CENTER_ALIGNMENT);
            legacy.add(icon);
            legacy.add(Box.createVerticalStrut(9));
            legacy.add(title);
            legacy.add(Box.createVerticalStrut(4));
            legacy.add(note);
            list.add(legacy);
        } else {
            for (int index = 0; index < events.size(); index++) {
                TicketEvent event = events.get(index);
                JPanel row = eventRow(event, index < events.size() - 1);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                list.add(row);
                if (index < events.size() - 1) list.add(Box.createVerticalStrut(7));
            }
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(Style.BORDER));
        scroll.getViewport().setBackground(Style.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(13);
        javax.swing.SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        return scroll;
    }

    private JPanel eventRow(TicketEvent event, boolean continued) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        row.add(new TimelineMarker(eventColor(event), continued), BorderLayout.WEST);
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        User eventActor = directory.byId(event.getActorId());
        JLabel title = Style.label(eventTitle(event), 12, true);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel meta = Style.label((eventActor == null ? event.getActorId() : eventActor.getName())
            + "  ·  " + date.format(event.getTimestamp()), 10, false);
        meta.setForeground(Style.MUTED);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(title);
        body.add(Box.createVerticalStrut(3));
        body.add(meta);
        String detail = eventDetail(event);
        if (detail != null) {
            JTextArea note = readOnlyText(detail);
            note.setForeground(Style.MUTED);
            note.setBorder(new EmptyBorder(7, 0, 2, 0));
            note.setAlignmentX(Component.LEFT_ALIGNMENT);
            note.setMaximumSize(new Dimension(Integer.MAX_VALUE, note.getPreferredSize().height));
            body.add(note);
        }
        row.add(body, BorderLayout.CENTER);
        return row;
    }

    private JPanel composer(Complaint ticket) {
        JPanel composer = new JPanel(new BorderLayout(9, 8));
        composer.setOpaque(false);
        composer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Style.BORDER),
            new EmptyBorder(13, 0, 0, 0)));
        String heading = actor.getRole() == Role.TECHNICIAN ? "Add progress update"
            : actor.getRole() == Role.ADMIN ? "Add administrative comment" : "Add a comment";
        composer.add(Style.label(heading, 11, true), BorderLayout.NORTH);
        JTextArea input = new JTextArea(3, 24);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        Style.field(input);
        JScrollPane inputScroll = new JScrollPane(input);
        inputScroll.setBorder(BorderFactory.createLineBorder(Style.BORDER));
        composer.add(inputScroll, BorderLayout.CENTER);
        ModernButton submit = Style.button(actor.getRole() == Role.TECHNICIAN ? "Add progress note" : "Add comment");
        boolean progressAllowed = actor.getRole() != Role.TECHNICIAN || ticket.getStatus() != Status.RESOLVED;
        submit.setEnabled(progressAllowed);
        if (!progressAllowed) submit.setToolTipText("Resolved tickets cannot receive progress notes");
        submit.addActionListener(event -> {
            String value = input.getText();
            runMutation(() -> {
                if (actor.getRole() == Role.TECHNICIAN) {
                    service.addProgressNote(actor, complaintId, value);
                } else {
                    service.addComment(actor, complaintId, value);
                }
            }, submit);
        });
        JPanel action = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        action.setOpaque(false);
        action.add(submit);
        composer.add(action, BorderLayout.SOUTH);
        return composer;
    }

    private void advanceStatus(Status target) {
        String resolutionNote = null;
        if (target == Status.RESOLVED) {
            JTextArea note = new JTextArea(5, 34);
            note.setLineWrap(true);
            note.setWrapStyleWord(true);
            Style.field(note);
            JPanel prompt = new JPanel(new BorderLayout(0, 9));
            prompt.add(Style.label("Resolution note (required)", 13, true), BorderLayout.NORTH);
            prompt.add(new JScrollPane(note), BorderLayout.CENTER);
            int answer = JOptionPane.showConfirmDialog(this, prompt, "Resolve ticket #" + complaintId,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) return;
            resolutionNote = note.getText();
        }
        String finalResolutionNote = resolutionNote;
        runMutation(() -> service.changeStatus(actor, complaintId, target, finalResolutionNote), null);
    }

    private void runMutation(Runnable mutation, ModernButton button) {
        if (button != null) {
            button.setEnabled(false);
            button.setText("Saving…");
        }
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                mutation.run();
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    if (onChanged != null) onChanged.run();
                    loadDetails();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("The operation was interrupted.");
                } catch (ExecutionException exception) {
                    showError(message(exception.getCause()));
                    if (button != null) {
                        button.setEnabled(true);
                        button.setText(actor.getRole() == Role.TECHNICIAN ? "Add progress note" : "Add comment");
                    }
                }
            }
        }.execute();
    }

    private RoundedPanel card() {
        RoundedPanel card = new RoundedPanel(16).withShadow();
        card.setBorder(new EmptyBorder(17, 18, 19, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JPanel detailGrid(String... values) {
        JPanel grid = new JPanel(new GridLayout(values.length / 4, 2, 14, 13));
        grid.setOpaque(false);
        for (int index = 0; index < values.length; index += 2) {
            JPanel cell = new JPanel();
            cell.setOpaque(false);
            cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
            JLabel label = Style.label(values[index].toUpperCase(Locale.ROOT), 9, true);
            label.setForeground(Style.MUTED);
            JLabel value = Style.label(values[index + 1], 11, true);
            cell.add(label);
            cell.add(Box.createVerticalStrut(3));
            cell.add(value);
            grid.add(cell);
        }
        return grid;
    }

    private JTextArea readOnlyText(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Style.font(11, false));
        area.setForeground(Style.TEXT);
        area.setRows(Math.max(1, Math.min(4, text.length() / 55 + 1)));
        return area;
    }

    private JPanel loadingPanel(String text) {
        JPanel panel = new JPanel(new java.awt.GridBagLayout());
        panel.setBackground(Style.BG);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        LoadingRing ring = new LoadingRing();
        ring.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel label = Style.label(text, 13, true);
        label.setForeground(Style.MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        copy.add(ring);
        copy.add(Box.createVerticalStrut(12));
        copy.add(label);
        panel.add(copy);
        return panel;
    }

    private void showLoadFailure(String message) {
        JPanel failure = loadingPanel("Unable to load complaint details");
        setContentPane(failure);
        revalidate();
        repaint();
        showError(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "CampusFix", JOptionPane.WARNING_MESSAGE);
    }

    private String eventTitle(TicketEvent event) {
        return switch (event.getEventType()) {
            case TICKET_CREATED -> "Complaint submitted";
            case TECHNICIAN_ASSIGNED -> "Technician assigned";
            case TECHNICIAN_REASSIGNED -> "Technician reassigned";
            case STATUS_CHANGED -> "Status updated";
            case COMMENT_ADDED -> "Comment added";
            case PROGRESS_UPDATED -> "Progress update";
            case TICKET_RESOLVED -> "Ticket resolved";
        };
    }

    private String eventDetail(TicketEvent event) {
        String note = event.getNote();
        return switch (event.getEventType()) {
            case TICKET_CREATED -> null;
            case TECHNICIAN_ASSIGNED, TECHNICIAN_REASSIGNED -> {
                User technician = directory.byId(note);
                yield technician == null ? "Assigned to " + note : "Assigned to " + technician.getName();
            }
            case STATUS_CHANGED -> {
                if (note == null || !note.contains("|")) yield note;
                String[] transition = note.split("\\|", 2);
                yield friendly(transition[0]) + " → " + friendly(transition[1]);
            }
            case COMMENT_ADDED, PROGRESS_UPDATED, TICKET_RESOLVED -> note;
        };
    }

    private Color eventColor(TicketEvent event) {
        return switch (event.getEventType()) {
            case TICKET_CREATED -> Style.BLUE;
            case TECHNICIAN_ASSIGNED, TECHNICIAN_REASSIGNED -> Style.PURPLE;
            case STATUS_CHANGED -> Style.AMBER;
            case COMMENT_ADDED -> Style.TEAL;
            case PROGRESS_UPDATED -> new Color(48, 134, 178);
            case TICKET_RESOLVED -> Style.GREEN;
        };
    }

    private static Status nextStatus(Status status) {
        return switch (status) {
            case ASSIGNED -> Status.IN_PROGRESS;
            case IN_PROGRESS -> Status.RESOLVED;
            default -> null;
        };
    }

    private static String friendly(String value) {
        String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String message(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null
            ? "The operation could not be completed." : throwable.getMessage();
    }

    private record DetailData(Complaint complaint, List<TicketEvent> events) { }

    private static final class TimelineMarker extends JPanel {
        private final Color color;
        private final boolean continued;
        private TimelineMarker(Color color, boolean continued) {
            this.color = color;
            this.continued = continued;
            setOpaque(false);
            setPreferredSize(new Dimension(18, 88));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (continued) {
                g.setColor(new Color(220, 229, 235));
                g.fillRoundRect(8, 16, 2, getHeight(), 2, 2);
            }
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 35));
            g.fillOval(1, 1, 16, 16);
            g.setColor(color);
            g.fillOval(6, 6, 6, 6);
            g.dispose();
        }
    }

    private static final class EmptyTimelineIcon extends JPanel {
        private EmptyTimelineIcon() {
            setOpaque(false);
            setPreferredSize(new Dimension(48, 48));
            setMaximumSize(new Dimension(48, 48));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(224, 245, 241));
            g.fillOval(0, 0, 48, 48);
            g.setColor(Style.TEAL_DARK);
            g.drawOval(13, 13, 22, 22);
            g.drawLine(24, 17, 24, 25);
            g.drawLine(24, 25, 29, 28);
            g.dispose();
        }
    }

    private static final class LoadingRing extends JPanel {
        private LoadingRing() {
            setOpaque(false);
            setPreferredSize(new Dimension(44, 44));
            setMaximumSize(new Dimension(44, 44));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setStroke(new java.awt.BasicStroke(4f));
            g.setColor(new Color(218, 229, 235));
            g.drawOval(5, 5, 34, 34);
            g.setColor(Style.TEAL);
            g.drawArc(5, 5, 34, 34, 40, 120);
            g.dispose();
        }
    }
}
