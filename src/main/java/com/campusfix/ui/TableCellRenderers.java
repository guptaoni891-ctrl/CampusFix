package com.campusfix.ui;

import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/** Renderers for readable enterprise-style tables. */
public final class TableCellRenderers {
    private TableCellRenderers() {
    }

    public static final class Standard extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setBorder(new EmptyBorder(4, 10, 4, 10));
            setFont(Style.font(12, column == 0));
            setForeground(Style.TEXT);
            setBackground(selected ? Style.SELECTION : row % 2 == 0 ? Style.WHITE : Style.ROW_ALT);
            return this;
        }
    }

    public static final class Badge extends JPanel implements TableCellRenderer {
        private final PillLabel pill = new PillLabel();

        public Badge() {
            super(new GridBagLayout());
            setOpaque(true);
            add(pill);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            setBackground(selected ? Style.SELECTION : row % 2 == 0 ? Style.WHITE : Style.ROW_ALT);
            if (value instanceof Status status) {
                pill.configure(status.name().replace('_', ' '), statusColor(status));
            } else if (value instanceof Severity severity) {
                pill.configure("●  " + severity.name(), severityColor(severity));
            } else {
                pill.configure(String.valueOf(value), Style.MUTED);
            }
            return this;
        }

        private static Color statusColor(Status status) {
            return switch (status) {
                case OPEN -> Style.AMBER;
                case ASSIGNED -> Style.BLUE;
                case IN_PROGRESS -> Style.PURPLE;
                case RESOLVED -> Style.GREEN;
            };
        }

        private static Color severityColor(Severity severity) {
            return switch (severity) {
                case LOW -> Style.GREEN;
                case MEDIUM -> Style.AMBER;
                case HIGH -> new Color(225, 103, 49);
                case CRITICAL -> Style.RED;
            };
        }
    }

    public static final class Priority extends JPanel implements TableCellRenderer {
        private int score;
        private boolean selected;
        private int row;

        public Priority() {
            setOpaque(false);
            setPreferredSize(new Dimension(90, 40));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                        boolean focused, int row, int column) {
            score = value instanceof Number number ? number.intValue() : 0;
            this.selected = selected;
            this.row = row;
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(selected ? Style.SELECTION : row % 2 == 0 ? Style.WHITE : Style.ROW_ALT);
            g.fillRect(0, 0, getWidth(), getHeight());
            int y = getHeight() / 2 - 3;
            int width = Math.max(20, getWidth() - 38);
            g.setColor(new Color(229, 235, 240));
            g.fillRoundRect(7, y, width, 6, 6, 6);
            g.setColor(score >= 75 ? Style.RED : score >= 50 ? Style.AMBER : Style.TEAL);
            g.fillRoundRect(7, y, Math.max(4, width * score / 100), 6, 6, 6);
            g.setFont(Style.font(11, true));
            g.setColor(Style.TEXT);
            g.drawString(String.valueOf(score), width + 13, y + 5);
            g.dispose();
        }
    }

    private static final class PillLabel extends JLabel {
        private Color accent = Style.MUTED;

        private PillLabel() {
            setHorizontalAlignment(CENTER);
            setFont(Style.font(10, true));
            setBorder(new EmptyBorder(5, 9, 5, 9));
            setOpaque(false);
        }

        private void configure(String text, Color color) {
            setText(text);
            accent = color;
            setForeground(color.darker());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
