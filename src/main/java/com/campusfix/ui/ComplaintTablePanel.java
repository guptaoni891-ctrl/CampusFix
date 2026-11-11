package com.campusfix.ui;

import com.campusfix.model.Complaint;
import com.campusfix.model.User;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.UserDirectory;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** Searchable complaint table with empty state and selection details. */
public final class ComplaintTablePanel extends RoundedPanel {
    private static final String TABLE_CARD = "table";
    private static final String EMPTY_CARD = "empty";
    private final ComplaintService service;
    private final UserDirectory directory;
    private final DateTimeFormatter date = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField search = new PlaceholderTextField("Search complaints…", 17);
    private final JComboBox<String> statusFilter = new JComboBox<>(
        new String[]{"All statuses", "Open", "Assigned", "In progress", "Resolved"});
    private final JTextArea details = new JTextArea();
    private final JPanel tableCards = new JPanel(new CardLayout());
    private final JLabel emptyTitle = Style.label("No complaints to show", 15, true);
    private final JLabel emptyMessage = Style.label("Try another search or status filter.", 12, false);
    private List<Complaint> source = List.of();
    private List<Complaint> filtered = new ArrayList<>();

    public ComplaintTablePanel(String title, ComplaintService service, UserDirectory directory) {
        super(new BorderLayout(0, 14), 18);
        this.service = service;
        this.directory = directory;
        withShadow();
        setBorder(new EmptyBorder(18, 18, 20, 18));

        add(toolbar(title), BorderLayout.NORTH);
        String[] columns = {"ID", "TITLE", "CATEGORY", "SEVERITY", "LOCATION", "PRIORITY", "STATUS", "TECHNICIAN", "CREATED"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                return switch (column) {
                    case 3 -> com.campusfix.model.Severity.class;
                    case 5 -> Integer.class;
                    case 6 -> com.campusfix.model.Status.class;
                    default -> String.class;
                };
            }
        };
        table = createTable();
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(Style.BORDER));
        tableScroll.getViewport().setBackground(Style.WHITE);
        tableCards.setOpaque(false);
        tableCards.add(tableScroll, TABLE_CARD);
        tableCards.add(emptyState(), EMPTY_CARD);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableCards, detailPanel());
        split.setResizeWeight(.79);
        split.setDividerSize(7);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshRows(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshRows(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshRows(); }
        });
        statusFilter.addActionListener(event -> refreshRows());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) updateDetails();
        });
    }

    public void setComplaints(List<Complaint> complaints) {
        source = List.copyOf(complaints);
        refreshRows();
    }

    public Complaint selectedComplaint() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = String.valueOf(model.getValueAt(modelRow, 0)).substring(1);
        int complaintId = Integer.parseInt(id);
        return filtered.stream().filter(ticket -> ticket.getId() == complaintId).findFirst().orElse(null);
    }

    private JPanel toolbar(String title) {
        JPanel toolbar = new JPanel(new BorderLayout(14, 0));
        toolbar.setOpaque(false);
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Style.label(title, 17, true));
        JLabel hint = Style.label("Search, sort, and select a row to inspect the full request.", 11, false);
        hint.setForeground(Style.MUTED);
        heading.add(Box.createVerticalStrut(3));
        heading.add(hint);
        toolbar.add(heading, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        controls.setOpaque(false);
        search.setToolTipText("Search by ID, title, category, location, or description");
        Style.field(search);
        search.setPreferredSize(new Dimension(210, 36));
        statusFilter.setToolTipText("Filter complaints by workflow status");
        Style.combo(statusFilter);
        statusFilter.setPreferredSize(new Dimension(135, 36));
        controls.add(search);
        controls.add(statusFilter);
        toolbar.add(controls, BorderLayout.EAST);
        return toolbar;
    }

    private JTable createTable() {
        JTable result = new JTable(model);
        result.setRowHeight(44);
        result.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        result.setAutoCreateRowSorter(true);
        result.setShowVerticalLines(false);
        result.setShowHorizontalLines(true);
        result.setGridColor(new Color(236, 241, 245));
        result.setSelectionBackground(Style.SELECTION);
        result.setSelectionForeground(Style.TEXT);
        result.setFillsViewportHeight(true);
        result.getTableHeader().setReorderingAllowed(false);
        result.getTableHeader().setPreferredSize(new Dimension(100, 38));
        result.getTableHeader().setBackground(new Color(239, 244, 247));
        result.getTableHeader().setForeground(Style.MUTED);
        result.getTableHeader().setFont(Style.font(10, true));
        result.setDefaultRenderer(Object.class, new TableCellRenderers.Standard());
        result.setDefaultRenderer(String.class, new TableCellRenderers.Standard());
        result.setDefaultRenderer(com.campusfix.model.Severity.class, new TableCellRenderers.Badge());
        result.setDefaultRenderer(com.campusfix.model.Status.class, new TableCellRenderers.Badge());
        result.setDefaultRenderer(Integer.class, new TableCellRenderers.Priority());
        result.getColumnModel().getColumn(0).setPreferredWidth(74);
        result.getColumnModel().getColumn(0).setMinWidth(70);
        result.getColumnModel().getColumn(0).setMaxWidth(82);
        result.getColumnModel().getColumn(1).setPreferredWidth(210);
        result.getColumnModel().getColumn(2).setPreferredWidth(95);
        result.getColumnModel().getColumn(3).setPreferredWidth(105);
        result.getColumnModel().getColumn(4).setPreferredWidth(135);
        result.getColumnModel().getColumn(5).setPreferredWidth(90);
        result.getColumnModel().getColumn(6).setPreferredWidth(115);
        result.getColumnModel().getColumn(7).setPreferredWidth(115);
        result.getColumnModel().getColumn(8).setPreferredWidth(130);
        return result;
    }

    private JPanel emptyState() {
        JPanel host = new JPanel(new GridBagLayout());
        host.setBackground(Style.WHITE);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        EmptyStateIcon symbol = new EmptyStateIcon();
        symbol.setAlignmentX(CENTER_ALIGNMENT);
        emptyTitle.setAlignmentX(CENTER_ALIGNMENT);
        emptyMessage.setForeground(Style.MUTED);
        emptyMessage.setAlignmentX(CENTER_ALIGNMENT);
        copy.add(symbol);
        copy.add(Box.createVerticalStrut(8));
        copy.add(emptyTitle);
        copy.add(Box.createVerticalStrut(4));
        copy.add(emptyMessage);
        host.add(copy);
        return host;
    }

    private JPanel detailPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 8, 0, 8));
        panel.add(Style.label("Selected complaint", 12, true), BorderLayout.NORTH);
        details.setText("Select a complaint to view its description, reporter, and latest activity.");
        details.setFont(Style.font(12, false));
        details.setForeground(Style.MUTED);
        details.setEditable(false);
        details.setOpaque(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setRows(3);
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private void refreshRows() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        String status = String.valueOf(statusFilter.getSelectedItem());
        filtered = source.stream()
            .filter(complaint -> query.isEmpty() || searchableText(complaint).contains(query))
            .filter(complaint -> status.equals("All statuses")
                || complaint.getStatus().name().replace('_', ' ').equalsIgnoreCase(status))
            .toList();
        model.setRowCount(0);
        for (Complaint complaint : filtered) {
            User technician = directory.byId(complaint.getTechnicianId());
            model.addRow(new Object[]{
                "#" + complaint.getId(), complaint.getTitle(), friendly(complaint.getCategory().name()),
                complaint.getSeverity(), complaint.getLocation(), service.score(complaint), complaint.getStatus(),
                technician == null ? "Not assigned" : technician.getName(), date.format(complaint.getCreatedAt())
            });
        }
        CardLayout layout = (CardLayout) tableCards.getLayout();
        if (filtered.isEmpty()) {
            emptyTitle.setText(source.isEmpty() ? "Nothing here yet" : "No matching complaints");
            emptyMessage.setText(source.isEmpty()
                ? "New complaints will appear here automatically."
                : "Try another search phrase or status filter.");
            layout.show(tableCards, EMPTY_CARD);
        } else {
            layout.show(tableCards, TABLE_CARD);
        }
        details.setText("Select a complaint to view its description, reporter, and latest activity.");
    }

    private String searchableText(Complaint complaint) {
        return (complaint.getId() + " " + complaint.getTitle() + " " + complaint.getDescription() + " "
            + complaint.getLocation() + " " + complaint.getCategory()).toLowerCase(Locale.ROOT);
    }

    private void updateDetails() {
        Complaint complaint = selectedComplaint();
        if (complaint == null) {
            details.setText("Select a complaint to view its description, reporter, and latest activity.");
            return;
        }
        User student = directory.byId(complaint.getStudentId());
        String reporter = student == null ? complaint.getStudentId() : student.getName();
        details.setText("Ticket #" + complaint.getId() + "  •  Reported by " + reporter
            + "  •  Priority " + service.score(complaint) + "/100\n"
            + complaint.getDescription() + "\n" + complaint.getLocation()
            + "  •  Updated " + date.format(complaint.getUpdatedAt()));
        details.setCaretPosition(0);
    }

    private static String friendly(String value) {
        String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static final class PlaceholderTextField extends JTextField {
        private final String placeholder;

        private PlaceholderTextField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !hasFocus()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(new Color(139, 154, 166));
                g.setFont(Style.font(12, false));
                g.drawString(placeholder, 12, (getHeight() + g.getFontMetrics().getAscent()) / 2 - 2);
                g.dispose();
            }
        }
    }

    private static final class EmptyStateIcon extends JPanel {
        private EmptyStateIcon() {
            setOpaque(false);
            setPreferredSize(new Dimension(42, 42));
            setMaximumSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(222, 245, 240));
            g.fillOval(0, 0, 42, 42);
            g.setColor(Style.TEAL_DARK);
            g.setStroke(new java.awt.BasicStroke(1.8f));
            g.drawRoundRect(13, 9, 16, 22, 3, 3);
            g.drawLine(17, 16, 25, 16);
            g.drawLine(17, 21, 25, 21);
            g.drawLine(17, 26, 23, 26);
            g.dispose();
        }
    }
}
