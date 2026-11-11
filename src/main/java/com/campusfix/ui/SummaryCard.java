package com.campusfix.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Reusable metric card for overview and analytics screens. */
public final class SummaryCard extends RoundedPanel {
    public SummaryCard(String heading, String value, String hint, Color accent) {
        super(new BorderLayout(14, 0), 18);
        withShadow();
        setBorder(new EmptyBorder(17, 18, 20, 18));
        setPreferredSize(new Dimension(190, 118));

        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        JLabel title = Style.label(heading.toUpperCase(), 10, true);
        title.setForeground(Style.MUTED);
        JLabel number = Style.label(value, 28, true);
        JLabel description = Style.label(hint, 11, false);
        description.setForeground(Style.MUTED);
        copy.add(title);
        copy.add(Box.createVerticalStrut(5));
        copy.add(number);
        copy.add(Box.createVerticalStrut(2));
        copy.add(description);
        add(copy, BorderLayout.CENTER);
        add(new AccentMark(accent), BorderLayout.EAST);
    }

    private static final class AccentMark extends JPanel {
        private final Color accent;
        private AccentMark(Color accent) {
            this.accent = accent;
            setOpaque(false);
            setPreferredSize(new Dimension(38, 38));
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
            g.fillRoundRect(0, 0, 38, 38, 12, 12);
            g.setColor(accent);
            g.fillOval(14, 14, 10, 10);
            g.dispose();
        }
    }
}
