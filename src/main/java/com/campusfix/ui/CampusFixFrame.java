package com.campusfix.ui;

import com.campusfix.model.Category;
import com.campusfix.model.Complaint;
import com.campusfix.model.Role;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import com.campusfix.model.Technician;
import com.campusfix.model.User;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.AnalyticsService;
import com.campusfix.service.ComplaintFilter;
import com.campusfix.service.ComplaintQueryService;
import com.campusfix.service.ReportService;
import com.campusfix.service.ReportType;
import com.campusfix.service.SlaStatus;
import com.campusfix.service.TechnicianRecommendation;
import com.campusfix.service.UserDirectory;
import com.campusfix.service.ValidationException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Main window and navigation shell for the CampusFix Swing client. */
public final class CampusFixFrame extends JFrame {
    private static final String OVERVIEW = "overview";
    private static final String COMPLAINTS = "complaints";
    private static final String CREATE = "create";
    private static final String TECHNICIANS = "technicians";
    private static final String ANALYTICS = "analytics";
    private static final String HISTORY = "history";

    private final ComplaintService service;
    private final UserDirectory directory;
    private final ComplaintQueryService queryService;
    private final AnalyticsService analyticsService;
    private final ReportService reportService;
    private final LoadingGlassPane loading = new LoadingGlassPane();
    private User currentUser;
    private SidebarPanel sidebar;
    private JPanel pageHost;
    private JLabel pageTitle;
    private JLabel pageSubtitle;
    private ComplaintTablePanel currentTable;
    private String currentPage = OVERVIEW;
    private boolean exportRunning;

    public CampusFixFrame(ComplaintService service, UserDirectory directory) {
        super("CampusFix | Smart Campus Operations");
        this.service = service;
        this.directory = directory;
        this.queryService = new ComplaintQueryService(service);
        this.analyticsService = new AnalyticsService(service, queryService, directory);
        this.reportService = new ReportService(service, queryService, analyticsService);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The shared theme still produces a consistent result without the system look and feel.
        }
        Style.installDefaults();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setGlassPane(loading);
        service.addListener((ticket, action) -> SwingUtilities.invokeLater(() -> {
            if (currentUser != null && pageHost != null) showPage(currentPage);
        }));
        loginView();
    }

    public void demoLogin(String username) {
        try {
            currentUser = directory.login(username, "demo123");
            dashboard();
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        }
    }

    private void loginView() {
        currentUser = null;
        currentTable = null;
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Style.BG);
        root.add(loginBrand());
        root.add(loginFormHost());
        setContentPane(root);
        revalidate();
        repaint();
    }

    private JPanel loginBrand() {
        GradientPanel panel = new GradientPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(40, 52, 40, 52));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel mark = new JLabel("CF", SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(Style.TEAL);
        mark.setForeground(Style.WHITE);
        mark.setFont(Style.font(20, true));
        mark.setPreferredSize(new Dimension(54, 54));
        mark.setMaximumSize(new Dimension(54, 54));
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(mark);
        copy.add(Box.createVerticalStrut(34));
        JLabel name = Style.label("CampusFix", 43, true);
        name.setForeground(Style.WHITE);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(name);
        copy.add(Box.createVerticalStrut(6));
        JLabel statement = Style.label("Campus maintenance, beautifully organized.", 17, false);
        statement.setForeground(new Color(192, 211, 225));
        statement.setAlignmentX(Component.LEFT_ALIGNMENT);
        copy.add(statement);
        copy.add(Box.createVerticalStrut(42));
        copy.add(featureLine("01", "Report issues with clarity"));
        copy.add(Box.createVerticalStrut(19));
        copy.add(featureLine("02", "Coordinate the right response"));
        copy.add(Box.createVerticalStrut(19));
        copy.add(featureLine("03", "Track work through resolution"));
        panel.add(copy);
        return panel;
    }

    private JPanel featureLine(String number, String text) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(390, 34));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel count = Style.label(number, 11, true);
        count.setForeground(new Color(99, 226, 194));
        count.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(99, 226, 194)));
        JLabel label = Style.label(text, 14, false);
        label.setForeground(new Color(226, 237, 244));
        row.add(count, BorderLayout.WEST);
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private JPanel loginFormHost() {
        JPanel host = new JPanel(new GridBagLayout());
        host.setBackground(Style.BG);
        host.setBorder(new EmptyBorder(28, 28, 28, 28));
        RoundedPanel form = new RoundedPanel(22).withShadow();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(38, 40, 36, 40));
        form.setPreferredSize(new Dimension(430, 490));

        JLabel eyebrow = Style.label("SECURE WORKSPACE", 10, true);
        eyebrow.setForeground(Style.TEAL_DARK);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel heading = Style.label("Welcome back", 28, true);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel caption = Style.label("Sign in to continue to campus operations.", 13, false);
        caption.setForeground(Style.MUTED);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(eyebrow);
        form.add(Box.createVerticalStrut(7));
        form.add(heading);
        form.add(Box.createVerticalStrut(7));
        form.add(caption);
        form.add(Box.createVerticalStrut(28));

        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        form.add(fieldLabel("Username"));
        form.add(Box.createVerticalStrut(7));
        configureLoginField(username);
        form.add(username);
        form.add(Box.createVerticalStrut(17));
        form.add(fieldLabel("Password"));
        form.add(Box.createVerticalStrut(7));
        configureLoginField(password);
        form.add(password);
        form.add(Box.createVerticalStrut(24));
        ModernButton signIn = Style.button("Sign in to CampusFix  →");
        signIn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        signIn.addActionListener(event -> signIn(username.getText(), new String(password.getPassword())));
        password.addActionListener(event -> signIn.doClick());
        form.add(signIn);
        form.add(Box.createVerticalStrut(23));
        JSeparator separator = new JSeparator();
        separator.setForeground(Style.BORDER);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        form.add(separator);
        form.add(Box.createVerticalStrut(17));
        JLabel demo = Style.label("Demo accounts", 11, true);
        demo.setForeground(Style.MUTED);
        demo.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(demo);
        JLabel accounts = Style.label("admin  •  student  •  student2  •  tech  •  tech2", 11, false);
        accounts.setForeground(Style.MUTED);
        accounts.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(accounts);
        JLabel passwordHint = Style.label("See the project README for demo sign-in.", 11, false);
        passwordHint.setForeground(Style.MUTED);
        passwordHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(passwordHint);
        host.add(form);
        return host;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = Style.label(text, 11, true);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void configureLoginField(JTextField field) {
        Style.field(field);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        field.setPreferredSize(new Dimension(300, 43));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private void signIn(String username, String password) {
        try {
            currentUser = directory.login(username, password);
            dashboard();
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        }
    }

    private void dashboard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Style.BG);
        sidebar = new SidebarPanel(navigationItems(), "Storage · " + service.persistenceMode(), this::showPage);
        root.add(sidebar, BorderLayout.WEST);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(Style.BG);
        workspace.add(topNavigation(), BorderLayout.NORTH);
        pageHost = new JPanel(new BorderLayout());
        pageHost.setBackground(Style.BG);
        pageHost.setBorder(new EmptyBorder(22, 24, 24, 24));
        workspace.add(pageHost, BorderLayout.CENTER);
        root.add(workspace, BorderLayout.CENTER);
        setContentPane(root);
        showPage(OVERVIEW);
        revalidate();
        repaint();
    }

    private List<SidebarPanel.Item> navigationItems() {
        return switch (currentUser.getRole()) {
            case STUDENT -> List.of(
                new SidebarPanel.Item(OVERVIEW, "Overview", NavigationIcon.Type.OVERVIEW),
                new SidebarPanel.Item(COMPLAINTS, "My Complaints", NavigationIcon.Type.TICKETS),
                new SidebarPanel.Item(CREATE, "Create Complaint", NavigationIcon.Type.ADD));
            case ADMIN -> List.of(
                new SidebarPanel.Item(OVERVIEW, "Overview", NavigationIcon.Type.OVERVIEW),
                new SidebarPanel.Item(COMPLAINTS, "All Complaints", NavigationIcon.Type.TICKETS),
                new SidebarPanel.Item(TECHNICIANS, "Technician Management", NavigationIcon.Type.USERS),
                new SidebarPanel.Item(ANALYTICS, "Analytics", NavigationIcon.Type.ANALYTICS));
            case TECHNICIAN -> List.of(
                new SidebarPanel.Item(OVERVIEW, "Overview", NavigationIcon.Type.OVERVIEW),
                new SidebarPanel.Item(COMPLAINTS, "Assigned Tickets", NavigationIcon.Type.TICKETS),
                new SidebarPanel.Item(HISTORY, "Work History", NavigationIcon.Type.HISTORY));
        };
    }

    private JPanel topNavigation() {
        JPanel top = new JPanel(new BorderLayout(20, 0));
        top.setBackground(Style.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Style.BORDER),
            new EmptyBorder(15, 25, 15, 22)));
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        pageTitle = Style.label("Overview", 21, true);
        pageSubtitle = Style.label("Your CampusFix workspace", 11, false);
        pageSubtitle.setForeground(Style.MUTED);
        titles.add(pageTitle);
        titles.add(Box.createVerticalStrut(3));
        titles.add(pageSubtitle);
        top.add(titles, BorderLayout.WEST);

        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 11, 0));
        account.setOpaque(false);
        account.add(new Avatar(initials(currentUser.getName())));
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(Style.label(currentUser.getName(), 12, true));
        JLabel role = Style.label(friendly(currentUser.getRole().name()), 10, false);
        role.setForeground(Style.MUTED);
        identity.add(role);
        account.add(identity);
        ModernButton logout = Style.ghost("Sign out");
        logout.setToolTipText("Sign out of this CampusFix session");
        logout.addActionListener(event -> loginView());
        account.add(logout);
        top.add(account, BorderLayout.EAST);
        return top;
    }

    private void showPage(String key) {
        if (currentUser == null || pageHost == null) return;
        currentPage = key;
        currentTable = null;
        sidebar.select(key);
        Component content;
        switch (key) {
            case COMPLAINTS -> {
                if (currentUser.getRole() == Role.TECHNICIAN) {
                    setPageHeading("Assigned Tickets", "Active requests currently assigned to you");
                    content = complaintWorkspace("Active work queue", ticket -> ticket.getStatus() != Status.RESOLVED);
                } else {
                    String title = currentUser.getRole() == Role.ADMIN ? "All Complaints" : "My Complaints";
                    setPageHeading(title, "Search, review, and manage campus maintenance requests");
                    content = complaintWorkspace(title, ticket -> true);
                }
            }
            case CREATE -> {
                setPageHeading("Create Complaint", "Give the maintenance team the details they need");
                content = createComplaintPage();
            }
            case TECHNICIANS -> {
                setPageHeading("Technician Management", "Monitor assignment capacity and completed work");
                content = new TechnicianPerformancePanel(analyticsService, currentUser);
            }
            case ANALYTICS -> {
                setPageHeading("Analytics", "Live operational patterns calculated from complaint data");
                content = new AnalyticsDashboardPanel(
                    analyticsService, reportService, directory, currentUser);
            }
            case HISTORY -> {
                setPageHeading("Work History", "Resolved tickets completed from your assigned queue");
                content = complaintWorkspace("Completed work", ticket -> ticket.getStatus() == Status.RESOLVED);
            }
            default -> {
                setPageHeading("Overview", roleOverviewSubtitle());
                content = overviewPage();
            }
        }
        pageHost.removeAll();
        pageHost.add(content, BorderLayout.CENTER);
        pageHost.revalidate();
        pageHost.repaint();
    }

    private void setPageHeading(String title, String subtitle) {
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);
    }

    private String roleOverviewSubtitle() {
        return switch (currentUser.getRole()) {
            case STUDENT -> "A clear view of your maintenance requests";
            case ADMIN -> "Campus-wide maintenance operations at a glance";
            case TECHNICIAN -> "Your active workload and completed requests";
        };
    }

    private JPanel overviewPage() {
        List<Complaint> tickets = service.visibleTo(currentUser);
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(welcomeBanner(), BorderLayout.NORTH);
        top.add(summaryCards(tickets), BorderLayout.CENTER);
        page.add(top, BorderLayout.NORTH);

        ComplaintTablePanel snapshot = new ComplaintTablePanel("Priority snapshot", service, directory);
        snapshot.setComplaints(tickets);
        snapshot.setOpenDetailsAction(this::openComplaintDetails);
        page.add(snapshot, BorderLayout.CENTER);
        return page;
    }

    private JPanel welcomeBanner() {
        RoundedPanel banner = new RoundedPanel(new BorderLayout(18, 0), 18);
        banner.setBackground(Style.NAVY_SOFT);
        banner.setBorder(new EmptyBorder(17, 22, 17, 20));
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel greeting = Style.label("Welcome, " + firstName(currentUser.getName()), 18, true);
        greeting.setForeground(Style.WHITE);
        JLabel insight = Style.label(overviewInsight(), 11, false);
        insight.setForeground(new Color(190, 209, 222));
        copy.add(greeting);
        copy.add(Box.createVerticalStrut(4));
        copy.add(insight);
        banner.add(copy, BorderLayout.CENTER);
        ModernButton quickAction = Style.button(switch (currentUser.getRole()) {
            case STUDENT -> "+ New complaint";
            case ADMIN -> "Open operations queue";
            case TECHNICIAN -> "View assigned work";
        });
        quickAction.addActionListener(event -> showPage(currentUser.getRole() == Role.STUDENT ? CREATE : COMPLAINTS));
        banner.add(quickAction, BorderLayout.EAST);
        return banner;
    }

    private String overviewInsight() {
        if (currentUser.getRole() == Role.ADMIN) {
            return "Pending queue: " + service.urgentQueue().size() + "  •  Overdue SLA: "
                + service.overdueCount(currentUser) + "  •  Recommendations include specialty and workload.";
        }
        if (currentUser.getRole() == Role.TECHNICIAN) {
            long active = service.visibleTo(currentUser).stream().filter(t -> t.getStatus() != Status.RESOLVED).count();
            return active == 0 ? "Your active queue is clear." : active + " active request(s) are ready for your attention.";
        }
        long open = service.visibleTo(currentUser).stream().filter(t -> t.getStatus() == Status.OPEN).count();
        return open == 0 ? "All of your submitted requests have entered the workflow." : open + " request(s) are awaiting assignment.";
    }

    private JPanel summaryCards(List<Complaint> tickets) {
        long open = tickets.stream().filter(ticket -> ticket.getStatus() == Status.OPEN).count();
        long workflow = tickets.stream().filter(ticket -> ticket.getStatus() == Status.ASSIGNED
            || ticket.getStatus() == Status.IN_PROGRESS).count();
        long resolved = tickets.stream().filter(ticket -> ticket.getStatus() == Status.RESOLVED).count();
        long overdue = tickets.stream().map(service::slaFor)
            .filter(sla -> sla.status() == SlaStatus.OVERDUE).count();
        JPanel cards = new JPanel(new GridLayout(1, 4, 13, 0));
        cards.setOpaque(false);
        cards.add(new SummaryCard("Total requests", String.valueOf(tickets.size()), "Visible in your account", Style.BLUE));
        cards.add(new SummaryCard("Open", String.valueOf(open), "Awaiting assignment", Style.AMBER));
        if (currentUser.getRole() == Role.ADMIN) {
            cards.add(new SummaryCard("Overdue SLA", String.valueOf(overdue), "Requires attention", Style.RED));
        } else {
            cards.add(new SummaryCard("In workflow", String.valueOf(workflow), "Assigned or in progress", Style.PURPLE));
        }
        cards.add(new SummaryCard("Resolved", String.valueOf(resolved), "Completed requests", Style.GREEN));
        return cards;
    }

    private JPanel complaintWorkspace(String title, Predicate<Complaint> pageFilter) {
        JPanel page = new JPanel(new BorderLayout(0, 13));
        page.setOpaque(false);
        page.add(complaintActions(), BorderLayout.NORTH);
        currentTable = new ComplaintTablePanel(title, service, directory);
        currentTable.setComplaints(service.visibleTo(currentUser).stream().filter(pageFilter).toList());
        currentTable.setOpenDetailsAction(this::openComplaintDetails);
        page.add(currentTable, BorderLayout.CENTER);
        return page;
    }

    private JPanel complaintActions() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JLabel status = Style.label("All changes are saved automatically", 11, false);
        status.setForeground(Style.MUTED);
        bar.add(status, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        ModernButton export = Style.secondary("Export CSV");
        export.setToolTipText("Export the complaints visible to your account");
        export.addActionListener(event -> exportCsv());
        ModernButton refresh = Style.secondary("Refresh");
        refresh.setToolTipText("Reload complaint data from storage");
        refresh.addActionListener(event -> refreshPageAsync());
        actions.add(export);
        actions.add(refresh);
        if (currentUser.getRole() == Role.STUDENT) {
            ModernButton create = Style.button("+ New complaint");
            create.addActionListener(event -> showPage(CREATE));
            actions.add(create);
        }
        if (currentUser.getRole() == Role.ADMIN) {
            ModernButton assign = Style.button("Assign technician");
            assign.setToolTipText("Assign the selected complaint to a technician");
            assign.addActionListener(event -> assignTicket());
            actions.add(assign);
        }
        if (currentUser.getRole() != Role.STUDENT) {
            ModernButton advance = Style.button("Advance status");
            advance.setToolTipText("Move the selected complaint to its next valid status");
            advance.addActionListener(event -> advanceStatus());
            actions.add(advance);
        }
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private Component createComplaintPage() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setOpaque(false);
        RoundedPanel form = new RoundedPanel(new BorderLayout(0, 20), 20).withShadow();
        form.setBorder(new EmptyBorder(24, 28, 25, 28));
        form.setPreferredSize(new Dimension(760, 545));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(Style.label("Request details", 18, true));
        JLabel hint = Style.label("Be specific so the maintenance team can act without delay.", 12, false);
        hint.setForeground(Style.MUTED);
        heading.add(Box.createVerticalStrut(4));
        heading.add(hint);
        form.add(heading, BorderLayout.NORTH);

        JTextField title = new JTextField();
        JTextField location = new JTextField();
        JTextArea description = new JTextArea(6, 30);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        javax.swing.JComboBox<Category> category = new javax.swing.JComboBox<>(Category.values());
        javax.swing.JComboBox<Severity> severity = new javax.swing.JComboBox<>(Severity.values());
        Style.field(title);
        Style.field(location);
        Style.field(description);
        Style.combo(category);
        Style.combo(severity);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 7, 12);
        constraints.gridx = 0; constraints.gridy = 0;
        fields.add(formLabel("Complaint title", "5–90 characters"), constraints);
        constraints.gridx = 1; constraints.insets = new Insets(0, 0, 7, 0);
        fields.add(formLabel("Building or room", "Where the issue is located"), constraints);
        constraints.gridy = 1; constraints.gridx = 0; constraints.insets = new Insets(0, 0, 17, 12);
        fields.add(title, constraints);
        constraints.gridx = 1; constraints.insets = new Insets(0, 0, 17, 0);
        fields.add(location, constraints);
        constraints.gridy = 2; constraints.gridx = 0; constraints.insets = new Insets(0, 0, 7, 12);
        fields.add(formLabel("Category", "Choose the closest match"), constraints);
        constraints.gridx = 1; constraints.insets = new Insets(0, 0, 7, 0);
        fields.add(formLabel("Severity", "Describe the operational impact"), constraints);
        constraints.gridy = 3; constraints.gridx = 0; constraints.insets = new Insets(0, 0, 17, 12);
        fields.add(category, constraints);
        constraints.gridx = 1; constraints.insets = new Insets(0, 0, 17, 0);
        fields.add(severity, constraints);
        constraints.gridy = 4; constraints.gridx = 0; constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 0, 7, 0);
        JPanel descriptionHeading = new JPanel(new BorderLayout());
        descriptionHeading.setOpaque(false);
        descriptionHeading.add(formLabel("Description", "10–1000 characters"), BorderLayout.WEST);
        JLabel count = Style.label("0 / 1000", 10, false);
        count.setForeground(Style.MUTED);
        descriptionHeading.add(count, BorderLayout.EAST);
        fields.add(descriptionHeading, constraints);
        constraints.gridy = 5; constraints.weighty = 1; constraints.fill = GridBagConstraints.BOTH;
        JScrollPane descriptionScroll = new JScrollPane(description);
        descriptionScroll.setBorder(BorderFactory.createLineBorder(Style.BORDER));
        fields.add(descriptionScroll, constraints);
        form.add(fields, BorderLayout.CENTER);

        description.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { count.setText(description.getText().length() + " / 1000"); }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        JLabel validation = Style.label("", 11, true);
        validation.setForeground(Style.RED);
        footer.add(validation, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        ModernButton cancel = Style.secondary("Cancel");
        cancel.addActionListener(event -> showPage(COMPLAINTS));
        ModernButton submit = Style.button("Submit complaint");
        submit.addActionListener(event -> {
            try {
                Complaint complaint = service.create(currentUser, title.getText(), description.getText(),
                    location.getText(), (Category) category.getSelectedItem(), (Severity) severity.getSelectedItem());
                showSuccess("Complaint #" + complaint.getId() + " submitted successfully. Priority: "
                    + service.score(complaint) + "/100.");
                showPage(COMPLAINTS);
            } catch (ValidationException exception) {
                validation.setText("●  " + exception.getMessage());
            } catch (IllegalStateException exception) {
                showError("The complaint could not be saved. " + exception.getMessage());
            }
        });
        buttons.add(cancel);
        buttons.add(submit);
        footer.add(buttons, BorderLayout.EAST);
        form.add(footer, BorderLayout.SOUTH);
        page.add(form);
        return scrollable(page);
    }

    private JPanel formLabel(String title, String helper) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.add(Style.label(title, 11, true), BorderLayout.WEST);
        JLabel hint = Style.label(helper, 10, false);
        hint.setForeground(Style.MUTED);
        panel.add(hint, BorderLayout.EAST);
        return panel;
    }

    private JPanel scrollable(Component content) {
        JPanel host = new JPanel(new BorderLayout());
        host.setOpaque(false);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        host.add(scroll, BorderLayout.CENTER);
        return host;
    }

    private void assignTicket() {
        Complaint complaint = selectedComplaint();
        if (complaint == null) {
            showError("Select a complaint in the table before assigning a technician.");
            return;
        }
        List<TechnicianRecommendation> recommendations;
        try {
            recommendations = service.recommendTechnicians(currentUser, complaint.getId());
        } catch (ValidationException exception) {
            showError(exception.getMessage());
            return;
        }
        if (recommendations.isEmpty()) {
            showError("No technicians are available for assignment.");
            return;
        }
        List<Technician> technicians = recommendations.stream()
            .map(TechnicianRecommendation::technician).toList();
        javax.swing.JComboBox<Technician> choices = new javax.swing.JComboBox<>(technicians.toArray(new Technician[0]));
        Style.combo(choices);
        choices.setAlignmentX(Component.LEFT_ALIGNMENT);
        TechnicianRecommendation first = recommendations.get(0);
        JPanel dialog = dialogForm("Assign ticket #" + complaint.getId(),
            first.isSpecialtyMatch() ? "Specialty match and active workload"
                : "No specialty match. Ranked by workload.");
        dialog.add(choices);
        dialog.add(Box.createVerticalStrut(10));
        JTextArea reason = new JTextArea(first.explanation());
        reason.setEditable(false);
        reason.setOpaque(false);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        reason.setFont(Style.font(11, false));
        reason.setForeground(Style.MUTED);
        reason.setRows(3);
        reason.setAlignmentX(Component.LEFT_ALIGNMENT);
        dialog.add(reason);
        choices.addActionListener(event -> {
            Technician selected = (Technician) choices.getSelectedItem();
            recommendations.stream().filter(item -> item.technician().equals(selected)).findFirst()
                .ifPresent(item -> reason.setText(item.explanation()));
        });
        int result = JOptionPane.showConfirmDialog(this, dialog, "Assign technician",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                service.assign(currentUser, complaint.getId(), (Technician) choices.getSelectedItem());
                Status preservedStatus = service.get(currentUser, complaint.getId()).getStatus();
                showSuccess("Technician assigned. Ticket #" + complaint.getId() + " remains "
                    + friendly(preservedStatus.name()) + ".");
            } catch (ValidationException exception) {
                showError(exception.getMessage());
            } catch (IllegalStateException exception) {
                showError("The assignment could not be saved. " + exception.getMessage());
            }
        }
    }

    private void advanceStatus() {
        Complaint complaint = selectedComplaint();
        if (complaint == null) {
            showError("Select a complaint in the table before changing its status.");
            return;
        }
        Status next = switch (complaint.getStatus()) {
            case ASSIGNED -> Status.IN_PROGRESS;
            case IN_PROGRESS -> Status.RESOLVED;
            default -> null;
        };
        if (next == null) {
            showError("This ticket must be ASSIGNED or IN PROGRESS before it can advance.");
            return;
        }
        String resolutionNote = null;
        if (next == Status.RESOLVED) {
            resolutionNote = requestResolutionNote(complaint.getId());
            if (resolutionNote == null) return;
        } else {
            int answer = JOptionPane.showConfirmDialog(this,
                "Move ticket #" + complaint.getId() + " from " + friendly(complaint.getStatus().name())
                    + " to " + friendly(next.name()) + "?",
                "Confirm status change", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
        }
        try {
            service.changeStatus(currentUser, complaint.getId(), next, resolutionNote);
            showSuccess("Ticket #" + complaint.getId() + " moved to " + friendly(next.name()) + ".");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (IllegalStateException exception) {
            showError("The status change could not be saved. " + exception.getMessage());
        }
    }

    private void openComplaintDetails(Complaint complaint) {
        ComplaintDetailDialog.open(this, service, directory, currentUser, complaint.getId(),
            () -> showPage(currentPage));
    }

    private String requestResolutionNote(int complaintId) {
        JTextArea note = new JTextArea(5, 34);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        Style.field(note);
        JPanel prompt = new JPanel(new BorderLayout(0, 9));
        prompt.add(Style.label("Resolution note (required)", 13, true), BorderLayout.NORTH);
        JLabel hint = Style.label("Describe the completed repair and verification in at least 10 characters.", 11, false);
        hint.setForeground(Style.MUTED);
        prompt.add(hint, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(note);
        scroll.setBorder(BorderFactory.createLineBorder(Style.BORDER));
        prompt.add(scroll, BorderLayout.SOUTH);
        int answer = JOptionPane.showConfirmDialog(this, prompt, "Resolve ticket #" + complaintId,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return answer == JOptionPane.OK_OPTION ? note.getText() : null;
    }

    private Complaint selectedComplaint() {
        return currentTable == null ? null : currentTable.selectedComplaint();
    }

    private JPanel dialogForm(String title, String caption) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 8, 8, 8));
        JLabel heading = Style.label(title, 15, true);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hint = Style.label(caption, 11, false);
        hint.setForeground(Style.MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension hintSize = hint.getPreferredSize();
        Dimension captionSize = new Dimension(hintSize.width + 24, hintSize.height);
        hint.setPreferredSize(captionSize);
        hint.setMinimumSize(captionSize);
        hint.setMaximumSize(captionSize);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(5));
        panel.add(hint);
        panel.add(Box.createVerticalStrut(16));
        return panel;
    }

    private void exportCsv() {
        if (exportRunning) return;
        ReportType[] available = currentUser.getRole() == Role.ADMIN
            ? ReportType.values()
            : new ReportType[]{ReportType.ALL_COMPLAINTS, ReportType.FILTERED_COMPLAINTS};
        javax.swing.JComboBox<ReportType> reportChoice = new javax.swing.JComboBox<>(available);
        if (currentTable != null) reportChoice.setSelectedItem(ReportType.FILTERED_COMPLAINTS);
        Style.combo(reportChoice);
        reportChoice.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel selection = dialogForm("Choose a CSV report",
            "Filtered exports follow the table.");
        selection.add(reportChoice);
        int selectionResult = JOptionPane.showConfirmDialog(this, selection, "Export report",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (selectionResult != JOptionPane.OK_OPTION) return;
        ReportType reportType = (ReportType) reportChoice.getSelectedItem();
        User actor = currentUser;
        if (reportType == ReportType.FILTERED_COMPLAINTS
            && (currentTable == null || !currentTable.resultsReady())) {
            showError("Apply the filters and wait for the table before exporting.");
            return;
        }
        List<Integer> selectedIds = currentTable == null || reportType != ReportType.FILTERED_COMPLAINTS
            ? List.of() : currentTable.visibleComplaintIds();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export CampusFix complaints");
        chooser.setSelectedFile(new java.io.File(switch (reportType) {
            case ALL_COMPLAINTS -> "campusfix_all_complaints.csv";
            case FILTERED_COMPLAINTS -> "campusfix_filtered_complaints.csv";
            case SLA_SUMMARY -> "campusfix_sla_summary.csv";
            case TECHNICIAN_SUMMARY -> "campusfix_technician_summary.csv";
        }));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path file = chooser.getSelectedFile().toPath();
            ComplaintFilter filter = currentTable == null
                ? ComplaintFilter.none() : currentTable.currentFilter();
            exportRunning = true;
            loading.showMessage("Exporting report…");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws IOException {
                    if (reportType == ReportType.FILTERED_COMPLAINTS) {
                        reportService.exportVisibleSelection(actor, file, selectedIds);
                    } else {
                        reportService.export(actor, file, reportType, filter);
                    }
                    return null;
                }

                @Override protected void done() {
                    try {
                        get();
                        showSuccess("Report exported to " + file.toAbsolutePath());
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        showError("The export was interrupted.");
                    } catch (ExecutionException exception) {
                        Throwable cause = exception.getCause();
                        showError("Unable to export the CSV file. "
                            + (cause == null ? exception.getMessage() : cause.getMessage()));
                    } finally {
                        exportRunning = false;
                        loading.hideOverlay();
                    }
                }
            }.execute();
        }
    }

    private void refreshPageAsync() {
        loading.showMessage("Refreshing complaint data…");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                service.visibleTo(currentUser);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    showPage(currentPage);
                } catch (Exception exception) {
                    showError("Unable to refresh complaint data. " + exception.getMessage());
                } finally {
                    loading.hideOverlay();
                }
            }
        }.execute();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "CampusFix", JOptionPane.WARNING_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "CampusFix", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String friendly(String value) {
        String lower = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        return (parts[0].substring(0, 1) + (parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : ""))
            .toUpperCase(Locale.ROOT);
    }

    private static String firstName(String name) {
        return name.trim().split("\\s+")[0];
    }

    private static final class GradientPanel extends JPanel {
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setPaint(new GradientPaint(0, 0, Style.NAVY, getWidth(), getHeight(), new Color(20, 65, 78)));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(71, 210, 179, 18));
            g.fillOval(getWidth() - 240, -90, 330, 330);
            g.fillOval(-130, getHeight() - 210, 300, 300);
            g.dispose();
        }
    }

    private static final class Avatar extends JPanel {
        private final String initials;
        private Avatar(String initials) {
            this.initials = initials;
            setOpaque(false);
            setPreferredSize(new Dimension(38, 38));
            setMinimumSize(new Dimension(38, 38));
            setMaximumSize(new Dimension(38, 38));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(222, 245, 240));
            g.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            g.setColor(Style.TEAL_DARK);
            g.setFont(Style.font(11, true));
            int width = g.getFontMetrics().stringWidth(initials);
            g.drawString(initials, (getWidth() - width) / 2, 24);
            g.dispose();
        }
    }

    private static final class LoadingGlassPane extends JPanel {
        private String message = "Loading…";
        private int angle;
        private final javax.swing.Timer timer;
        private LoadingGlassPane() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            timer = new javax.swing.Timer(70, event -> { angle = (angle + 24) % 360; repaint(); });
            addMouseListener(new java.awt.event.MouseAdapter() { });
            setVisible(false);
        }
        private void showMessage(String text) {
            message = text;
            setVisible(true);
            timer.start();
        }
        private void hideOverlay() {
            timer.stop();
            setVisible(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(15, 35, 55, 82));
            g.fillRect(0, 0, getWidth(), getHeight());
            int cardWidth = 250;
            int cardHeight = 92;
            int x = (getWidth() - cardWidth) / 2;
            int y = (getHeight() - cardHeight) / 2;
            g.setColor(Style.WHITE);
            g.fillRoundRect(x, y, cardWidth, cardHeight, 18, 18);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(214, 227, 232));
            g.drawArc(x + 22, y + 28, 30, 30, 0, 360);
            g.setColor(Style.TEAL);
            g.drawArc(x + 22, y + 28, 30, 30, angle, 105);
            g.setFont(Style.font(12, true));
            g.setColor(Style.TEXT);
            g.drawString(message, x + 67, y + 50);
            g.dispose();
        }
    }
}
