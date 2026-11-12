package com.campusfix.ui;

import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

/** Rounded status/severity badge for detail views and dialogs. */
public final class StatusBadge extends JLabel {
    private final Color accent;

    public StatusBadge(Status status) {
        this(status.name().replace('_', ' '), switch (status) {
            case OPEN -> Style.AMBER;
            case ASSIGNED -> Style.BLUE;
            case IN_PROGRESS -> Style.PURPLE;
            case RESOLVED -> Style.GREEN;
        });
    }

    public StatusBadge(Severity severity) {
        this("●  " + severity.name(), switch (severity) {
            case LOW -> Style.GREEN;
            case MEDIUM -> Style.AMBER;
            case HIGH -> new Color(225, 103, 49);
            case CRITICAL -> Style.RED;
        });
    }

    private StatusBadge(String text, Color accent) {
        super(text, CENTER);
        this.accent = accent;
        setFont(Style.font(10, true));
        setForeground(accent.darker());
        setBorder(new EmptyBorder(6, 11, 6, 11));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 30));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g.dispose();
        super.paintComponent(graphics);
    }
}
