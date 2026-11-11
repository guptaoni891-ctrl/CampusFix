package com.campusfix.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/** Rounded, dependency-free Swing button with hover and pressed states. */
public final class ModernButton extends JButton {
    public enum Variant { PRIMARY, SECONDARY, GHOST }

    private final Variant variant;
    private boolean hovered;
    private boolean pressed;

    public ModernButton(String text, Variant variant) {
        super(text);
        this.variant = variant;
        setFont(Style.font(12, true));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 16, 10, 16));
        setForeground(variant == Variant.PRIMARY ? Style.WHITE : Style.TEXT);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color background;
        if (!isEnabled()) {
            background = new Color(196, 207, 214);
        } else if (variant == Variant.PRIMARY) {
            background = pressed ? Style.TEAL_DARK : hovered ? new Color(18, 172, 151) : Style.TEAL;
        } else if (variant == Variant.SECONDARY) {
            background = hovered ? new Color(239, 247, 246) : Style.WHITE;
        } else {
            background = hovered ? new Color(234, 240, 244) : new Color(0, 0, 0, 0);
        }
        g.setColor(background);
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        if (variant == Variant.SECONDARY) {
            g.setColor(hovered ? new Color(167, 210, 203) : Style.BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
