package com.campusfix.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Role-aware left navigation shared by all dashboard pages. */
public final class SidebarPanel extends JPanel {
    public record Item(String key, String label, NavigationIcon.Type icon) { }

    private final Map<String, NavButton> buttons = new LinkedHashMap<>();

    public SidebarPanel(List<Item> items, String storageMode, Consumer<String> onNavigate) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(252, 680));
        setBackground(Style.NAVY);
        setBorder(new EmptyBorder(24, 16, 18, 16));

        add(brand(), BorderLayout.NORTH);
        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        JLabel heading = Style.label("WORKSPACE", 10, true);
        heading.setForeground(new Color(139, 165, 185));
        heading.setBorder(new EmptyBorder(28, 12, 10, 0));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        navigation.add(heading);
        for (Item item : items) {
            NavButton button = new NavButton(item.label(), item.icon());
            button.addActionListener(event -> onNavigate.accept(item.key()));
            button.setAlignmentX(LEFT_ALIGNMENT);
            buttons.put(item.key(), button);
            navigation.add(button);
            navigation.add(Box.createVerticalStrut(5));
        }
        add(navigation, BorderLayout.CENTER);

        RoundedPanel storage = new RoundedPanel(new BorderLayout(0, 4), 14);
        storage.setBackground(Style.NAVY_SOFT);
        storage.setBorder(new EmptyBorder(12, 13, 12, 13));
        JLabel online = Style.label("●  SYSTEM READY", 10, true);
        online.setForeground(new Color(93, 218, 184));
        JLabel mode = Style.label(storageMode, 11, false);
        mode.setForeground(new Color(181, 199, 213));
        storage.add(online, BorderLayout.NORTH);
        storage.add(mode, BorderLayout.CENTER);
        add(storage, BorderLayout.SOUTH);
    }

    public void select(String key) {
        buttons.forEach((buttonKey, button) -> button.setSelected(buttonKey.equals(key)));
    }

    private JPanel brand() {
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("CF", JLabel.CENTER);
        mark.setPreferredSize(new Dimension(42, 42));
        mark.setOpaque(true);
        mark.setBackground(Style.TEAL);
        mark.setForeground(Style.WHITE);
        mark.setFont(Style.font(15, true));
        mark.setBorder(BorderFactory.createEmptyBorder());
        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel name = Style.label("CampusFix", 20, true);
        name.setForeground(Style.WHITE);
        JLabel caption = Style.label("Campus operations", 10, false);
        caption.setForeground(new Color(154, 178, 197));
        words.add(name);
        words.add(caption);
        brand.add(mark, BorderLayout.WEST);
        brand.add(words, BorderLayout.CENTER);
        return brand;
    }

    private static final class NavButton extends JButton {
        private final NavigationIcon.Type type;
        private boolean active;

        private NavButton(String text, NavigationIcon.Type type) {
            super(text);
            this.type = type;
            setHorizontalAlignment(LEFT);
            setIconTextGap(12);
            setFont(Style.font(13, false));
            setForeground(new Color(194, 210, 222));
            setBorder(new EmptyBorder(12, 13, 12, 13));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
            setPreferredSize(new Dimension(220, 45));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setIcon(new NavigationIcon(type, new Color(151, 177, 197)));
        }

        @Override
        public void setSelected(boolean selected) {
            active = selected;
            setForeground(selected ? Style.WHITE : new Color(194, 210, 222));
            setFont(Style.font(13, selected));
            setIcon(new NavigationIcon(type, selected ? new Color(100, 227, 195) : new Color(151, 177, 197)));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            if (active || getModel().isRollover()) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(active ? new Color(35, 76, 96) : new Color(28, 61, 84));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (active) {
                    g.setColor(Style.TEAL);
                    g.fillRoundRect(0, 8, 3, getHeight() - 16, 3, 3);
                }
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
