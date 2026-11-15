package com.campusfix.ui;

import com.campusfix.model.Category;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import com.campusfix.model.Technician;
import com.campusfix.service.ComplaintFilter;
import com.campusfix.service.ComplaintSort;
import com.campusfix.service.SlaStatus;
import com.campusfix.service.UserDirectory;
import com.campusfix.service.ValidationException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Reusable advanced complaint filter controls; parsing and criteria live outside data tables. */
public final class ComplaintFilterBar extends JPanel {
    private final JTextField search = new JTextField(15);
    private final JComboBox<Choice<Status>> status = new JComboBox<>();
    private final JComboBox<Choice<Severity>> severity = new JComboBox<>();
    private final JComboBox<Choice<Category>> category = new JComboBox<>();
    private final JComboBox<Choice<SlaStatus>> sla = new JComboBox<>();
    private final JComboBox<Choice<String>> technician = new JComboBox<>();
    private final JTextField from = new JTextField(8);
    private final JTextField to = new JTextField(8);
    private final JComboBox<ComplaintSort> sort = new JComboBox<>(ComplaintSort.values());
    private final ModernButton apply = Style.button("Apply filters");
    private final ModernButton clear = Style.secondary("Clear");
    private final JLabel validation = Style.label("", 10, true);
    private ComplaintFilter currentFilter = ComplaintFilter.none();
    private ComplaintSort currentSort = ComplaintSort.PRIORITY_DESC;
    private BiConsumer<ComplaintFilter, ComplaintSort> listener;

    public ComplaintFilterBar(UserDirectory directory) {
        super(new BorderLayout(0, 5));
        setOpaque(false);
        Objects.requireNonNull(directory, "directory");
        populateChoices(directory);
        configureControls();

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(firstRow());
        rows.add(Box.createVerticalStrut(5));
        rows.add(secondRow());
        add(rows, BorderLayout.CENTER);
        validation.setForeground(Style.RED);
        add(validation, BorderLayout.SOUTH);

        apply.addActionListener(event -> applySelection());
        clear.addActionListener(event -> clear());
        search.addActionListener(event -> applySelection());
    }

    public void setListener(BiConsumer<ComplaintFilter, ComplaintSort> listener) {
        this.listener = listener;
    }

    public ComplaintFilter currentFilter() {
        return currentFilter;
    }

    public ComplaintSort currentSort() {
        return currentSort;
    }

    public void setLoading(boolean loading) {
        apply.setEnabled(!loading);
        clear.setEnabled(!loading);
        apply.setText(loading ? "Applying…" : "Apply filters");
    }

    public void clear() {
        search.setText("");
        status.setSelectedIndex(0);
        severity.setSelectedIndex(0);
        category.setSelectedIndex(0);
        sla.setSelectedIndex(0);
        technician.setSelectedIndex(0);
        from.setText("");
        to.setText("");
        sort.setSelectedItem(ComplaintSort.PRIORITY_DESC);
        validation.setText("");
        applySelection();
    }

    private JPanel firstRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        row.setOpaque(false);
        search.setToolTipText("Search by ticket ID, title, or location");
        search.setPreferredSize(new Dimension(180, 34));
        row.add(labeled("SEARCH", search));
        row.add(labeled("STATUS", status));
        row.add(labeled("SEVERITY", severity));
        row.add(labeled("CATEGORY", category));
        row.add(labeled("SLA", sla));
        return row;
    }

    private JPanel secondRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        row.setOpaque(false);
        technician.setPreferredSize(new Dimension(155, 34));
        from.setToolTipText("Inclusive start date in YYYY-MM-DD format");
        to.setToolTipText("Inclusive end date in YYYY-MM-DD format");
        sort.setPreferredSize(new Dimension(178, 34));
        row.add(labeled("TECHNICIAN", technician));
        row.add(labeled("FROM YYYY-MM-DD", from));
        row.add(labeled("TO YYYY-MM-DD", to));
        row.add(labeled("SORT", sort));
        row.add(apply);
        row.add(clear);
        return row;
    }

    private JPanel labeled(String text, java.awt.Component component) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = Style.label(text, 8, true);
        label.setForeground(Style.MUTED);
        panel.add(label);
        panel.add(Box.createVerticalStrut(2));
        panel.add(component);
        return panel;
    }

    private void configureControls() {
        Style.field(search);
        Style.field(from);
        Style.field(to);
        for (JComboBox<?> combo : new JComboBox<?>[]{status, severity, category, sla, technician, sort}) {
            Style.combo(combo);
            if (combo.getPreferredSize().width < 112) combo.setPreferredSize(new Dimension(112, 34));
        }
    }

    private void populateChoices(UserDirectory directory) {
        status.addItem(new Choice<>("All statuses", null));
        for (Status value : Status.values()) status.addItem(new Choice<>(friendly(value.name()), value));
        severity.addItem(new Choice<>("All severities", null));
        for (Severity value : Severity.values()) severity.addItem(new Choice<>(friendly(value.name()), value));
        category.addItem(new Choice<>("All categories", null));
        for (Category value : Category.values()) category.addItem(new Choice<>(friendly(value.name()), value));
        sla.addItem(new Choice<>("All SLA states", null));
        for (SlaStatus value : SlaStatus.values()) sla.addItem(new Choice<>(friendly(value.name()), value));
        technician.addItem(new Choice<>("All technicians", null));
        technician.addItem(new Choice<>("Unassigned", ComplaintFilter.UNASSIGNED));
        for (Technician value : directory.technicians()) {
            technician.addItem(new Choice<>(value.getName(), value.getId()));
        }
    }

    private void applySelection() {
        try {
            ComplaintFilter filter = new ComplaintFilter(
                selected(status), selected(severity), selected(category),
                parseDate(from.getText(), "start"), parseDate(to.getText(), "end"),
                selected(sla), selected(technician), search.getText());
            currentFilter = filter;
            currentSort = (ComplaintSort) sort.getSelectedItem();
            validation.setText("");
            if (listener != null) listener.accept(currentFilter, currentSort);
        } catch (ValidationException exception) {
            validation.setText("●  " + exception.getMessage());
        }
    }

    private static LocalDate parseDate(String value, String label) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ValidationException("Enter the " + label + " date as YYYY-MM-DD.");
        }
    }

    private static <T> T selected(JComboBox<Choice<T>> combo) {
        int index = combo.getSelectedIndex();
        Choice<T> choice = index < 0 ? null : combo.getItemAt(index);
        return choice == null ? null : choice.value();
    }

    private static String friendly(String value) {
        String lower = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private record Choice<T>(String label, T value) {
        @Override public String toString() { return label; }
    }
}
