package com.voting.gui;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Shared styling constants, factory methods, and reusable custom components
 * for the Online Voting System GUI.
 *
 * <p>Every panel references this class for colors, fonts, and widget
 * factories so the dark-charcoal theme is consistent across all screens.</p>
 */
public final class UIConstants {

    private UIConstants() {}

    // ── Colors ──────────────────────────────────────────────────────────

    public static final Color BG_DARK        = new Color(0x12, 0x14, 0x1D);
    public static final Color BG_CARD        = new Color(0x1A, 0x1D, 0x2B);
    public static final Color BG_FIELD       = new Color(0x22, 0x25, 0x36);
    public static final Color PRIMARY_BLUE   = new Color(0x3B, 0x82, 0xF6);
    public static final Color PRIMARY_HOVER  = new Color(0x2B, 0x6C, 0xE0);
    public static final Color BTN_DARK       = new Color(0x1E, 0x21, 0x30);
    public static final Color BTN_DARK_HOVER = new Color(0x28, 0x2C, 0x3E);
    public static final Color BTN_BORDER     = new Color(0x33, 0x37, 0x4D);
    public static final Color TEXT_PRIMARY   = new Color(0xF1, 0xF5, 0xF9);
    public static final Color TEXT_SECONDARY = new Color(0x94, 0xA3, 0xB8);
    public static final Color TEXT_MUTED     = new Color(0x64, 0x74, 0x8B);
    public static final Color ACCENT_GREEN   = new Color(0x22, 0xC5, 0x5E);
    public static final Color DANGER_RED     = new Color(0xEF, 0x44, 0x44);
    public static final Color DANGER_HOVER   = new Color(0xDC, 0x26, 0x26);
    public static final Color STATUS_UPCOMING  = new Color(0xF5, 0x9E, 0x0B);
    public static final Color STATUS_ACTIVE    = ACCENT_GREEN;
    public static final Color STATUS_COMPLETED = TEXT_MUTED;
    public static final Color TABLE_ROW_ALT  = new Color(0x16, 0x18, 0x24);
    public static final Color TABLE_GRID     = new Color(0x2A, 0x2D, 0x3E);

    // ── Fonts ───────────────────────────────────────────────────────────

    public static final Font FONT_TITLE        = new Font("Segoe UI", Font.BOLD,  28);
    public static final Font FONT_HEADER       = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_SUBTITLE     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BUTTON       = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_BODY         = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL        = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_LABEL        = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font FONT_FIELD        = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font FONT_TABLE        = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_STAT_VALUE   = new Font("Segoe UI", Font.BOLD,  18);
    public static final Font FONT_STAT_LABEL   = new Font("Segoe UI", Font.PLAIN, 11);

    // ═══════════════════════════════════════════════════════════════════
    //  Button Factories
    // ═══════════════════════════════════════════════════════════════════

    public static JButton createPrimaryButton(String text) {
        return new StyledButton(text, PRIMARY_BLUE, PRIMARY_HOVER, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return new StyledButton(text, BTN_DARK, BTN_DARK_HOVER, TEXT_PRIMARY);
    }

    public static JButton createDangerButton(String text) {
        return new StyledButton(text, DANGER_RED, DANGER_HOVER, Color.WHITE);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Field / Label Factories
    // ═══════════════════════════════════════════════════════════════════

    /** Creates a dark-themed text field with rounded border. */
    public static JTextField createStyledField() {
        JTextField field = new JTextField();
        field.setFont(FONT_FIELD);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_FIELD);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BTN_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(400, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return field;
    }

    /** Small bold label used above form fields. */
    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** Muted supporting text. */
    public static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Table Factory
    // ═══════════════════════════════════════════════════════════════════

    /** Creates a styled, non-editable JTable with alternating dark rows. */
    public static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT_TABLE);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_DARK);
        table.setSelectionBackground(new Color(0x3B, 0x82, 0xF6, 0x30));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(TABLE_GRID);
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setForeground(TEXT_SECONDARY);
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TABLE_GRID));
        header.setReorderingAllowed(false);

        // Alternating-row renderer (default for Object.class)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean selected, boolean focused, int row, int col) {
                super.getTableCellRendererComponent(t, val, selected, focused, row, col);
                if (!selected) {
                    setBackground(row % 2 == 0 ? BG_DARK : TABLE_ROW_ALT);
                }
                setForeground(TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });

        return table;
    }

    /** Wraps a JTable in a dark-bordered scroll pane. */
    public static JScrollPane wrapInScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(TABLE_GRID));
        sp.getViewport().setBackground(BG_DARK);
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Header Panel
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a standard screen header with a title on the left
     * and an optional back-link on the right.
     */
    public static JPanel createHeaderPanel(String title,
                                           String backText,
                                           Runnable onBack) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_PRIMARY);
        header.add(titleLabel, BorderLayout.WEST);

        if (backText != null && onBack != null) {
            JButton backBtn = new JButton(backText);
            backBtn.setFont(FONT_BODY);
            backBtn.setForeground(TEXT_SECONDARY);
            backBtn.setBorderPainted(false);
            backBtn.setContentAreaFilled(false);
            backBtn.setFocusPainted(false);
            backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            backBtn.addActionListener(e -> onBack.run());
            backBtn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    backBtn.setForeground(TEXT_PRIMARY);
                }
                @Override public void mouseExited(MouseEvent e) {
                    backBtn.setForeground(TEXT_SECONDARY);
                }
            });
            header.add(backBtn, BorderLayout.EAST);
        }

        return header;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Layout Helpers
    // ═══════════════════════════════════════════════════════════════════

    /** Wraps a button in a panel that stretches it horizontally. */
    public static JPanel wrapButton(JButton button, int height) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.add(button, BorderLayout.CENTER);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Reusable Custom Components
    // ═══════════════════════════════════════════════════════════════════

    /**
     * A panel with rounded corners and a solid background color.
     */
    public static class RoundedPanel extends JPanel {
        private final int    radius;
        private final Color  bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius  = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), radius, radius));
            // Subtle border
            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 0x08));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(
                    0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * A styled button with rounded corners and hover color change.
     */
    public static class StyledButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private boolean hovering = false;

        public StyledButton(String text, Color normalBg, Color hoverBg,
                            Color textColor) {
            super(text);
            this.normalBg = normalBg;
            this.hoverBg  = hoverBg;

            setFont(FONT_BUTTON);
            setForeground(textColor);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 44));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    hovering = true;  repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    hovering = false; repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovering ? hoverBg : normalBg);
            g2.fill(new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), 10, 10));
            // Border for secondary (dark) buttons
            if (normalBg.equals(BTN_DARK)) {
                g2.setColor(BTN_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(
                        0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
