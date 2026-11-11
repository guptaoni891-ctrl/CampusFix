package com.campusfix.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

/** Shared visual language for the CampusFix desktop client. */
public final class Style {
    public static final Color NAVY = new Color(15, 35, 55);
    public static final Color NAVY_SOFT = new Color(24, 53, 78);
    public static final Color TEAL = new Color(15, 157, 138);
    public static final Color TEAL_DARK = new Color(10, 126, 112);
    public static final Color BLUE = new Color(48, 111, 205);
    public static final Color BG = new Color(244, 247, 250);
    public static final Color WHITE = Color.WHITE;
    public static final Color TEXT = new Color(24, 43, 58);
    public static final Color MUTED = new Color(101, 120, 136);
    public static final Color BORDER = new Color(220, 229, 236);
    public static final Color ROW_ALT = new Color(248, 251, 253);
    public static final Color SELECTION = new Color(229, 246, 243);
    public static final Color GREEN = new Color(23, 148, 107);
    public static final Color AMBER = new Color(218, 143, 32);
    public static final Color RED = new Color(207, 67, 67);
    public static final Color PURPLE = new Color(116, 91, 184);

    private static final String FONT_FAMILY = findFont();

    private Style() {
    }

    public static void installDefaults() {
        UIManager.put("Label.font", font(13, false));
        UIManager.put("Button.font", font(13, true));
        UIManager.put("TextField.font", font(13, false));
        UIManager.put("TextArea.font", font(13, false));
        UIManager.put("ComboBox.font", font(13, false));
        UIManager.put("Table.font", font(12, false));
        UIManager.put("TableHeader.font", font(11, true));
        UIManager.put("OptionPane.messageFont", font(13, false));
        UIManager.put("OptionPane.buttonFont", font(12, true));
        UIManager.put("ToolTip.font", font(12, false));
        UIManager.put("ToolTip.background", NAVY);
        UIManager.put("ToolTip.foreground", WHITE);
        UIManager.put("ToolTip.border", new EmptyBorder(7, 9, 7, 9));
        UIManager.put("ProgressBar.foreground", TEAL);
        UIManager.put("ProgressBar.background", new Color(231, 238, 243));
    }

    private static String findFont() {
        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (family.equalsIgnoreCase("Segoe UI")) {
                return family;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font font(int size, boolean bold) {
        return new Font(FONT_FAMILY, bold ? Font.BOLD : Font.PLAIN, size);
    }

    public static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, bold));
        label.setForeground(TEXT);
        return label;
    }

    public static JPanel panel() {
        JPanel panel = new JPanel();
        panel.setBackground(WHITE);
        return panel;
    }

    public static ModernButton button(String label) {
        return new ModernButton(label, ModernButton.Variant.PRIMARY);
    }

    public static ModernButton secondary(String label) {
        return new ModernButton(label, ModernButton.Variant.SECONDARY);
    }

    public static ModernButton ghost(String label) {
        return new ModernButton(label, ModernButton.Variant.GHOST);
    }

    public static void field(JTextComponent field) {
        field.setFont(font(13, false));
        field.setForeground(TEXT);
        field.setCaretColor(TEAL_DARK);
        field.setBackground(WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(10, 12, 10, 12)
        ));
    }

    public static void combo(JComboBox<?> combo) {
        combo.setFont(font(13, false));
        combo.setForeground(TEXT);
        combo.setBackground(WHITE);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void progress(JProgressBar progressBar) {
        progressBar.setBorderPainted(false);
        progressBar.setBackground(new Color(231, 238, 243));
        progressBar.setForeground(TEAL);
    }
}
