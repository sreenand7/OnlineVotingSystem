package com.voting.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Home screen — the first thing the user sees.
 *
 * <p>Centered card with title, subtitle, and three action buttons
 * matching the Figma dark-charcoal design.</p>
 */
public class HomePanel extends JPanel {

    private final VotingAppFrame frame;

    public HomePanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        // ── Ballot-box icon (rendered via Java2D) ───────────────────────
        JLabel iconLabel = new JLabel(createBallotIcon());
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(24));

        // ── Title ───────────────────────────────────────────────────────
        JLabel title = new JLabel("Online Voting System");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        // ── Subtitle ────────────────────────────────────────────────────
        JLabel subtitle = new JLabel("Election Management & Voting");
        subtitle.setFont(UIConstants.FONT_SUBTITLE);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(40));

        // ── Buttons ─────────────────────────────────────────────────────
        JButton btnStart = UIConstants.createPrimaryButton("Start / Select Election");
        btnStart.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_ELECTION_SELECT));
        card.add(UIConstants.wrapButton(btnStart, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnHistory = UIConstants.createSecondaryButton("View Previous Elections");
        btnHistory.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS));
        card.add(UIConstants.wrapButton(btnHistory, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnExit = UIConstants.createSecondaryButton("Exit");
        btnExit.addActionListener(e -> { frame.dispose(); System.exit(0); });
        card.add(UIConstants.wrapButton(btnExit, 48));
        card.add(Box.createVerticalStrut(32));

        // ── Footer ──────────────────────────────────────────────────────
        JLabel footer = new JLabel("Secure  |  Fair  |  Transparent");
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(UIConstants.TEXT_MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(footer);

        // Center the card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    // ── Ballot-box icon ─────────────────────────────────────────────────

    private Icon createBallotIcon() {
        final int size = 56;
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 0x20));
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 0x40));
                g2.fillOval(x + 8, y + 8, size - 16, size - 16);
                g2.setColor(UIConstants.PRIMARY_BLUE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                int bx = x + 16, by = y + 18, bw = 24, bh = 20;
                g2.drawRoundRect(bx, by, bw, bh, 3, 3);
                g2.drawLine(bx + 8, by, bx + 16, by);
                g2.drawLine(bx + 8, by - 2, bx + 16, by - 2);
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                int px = bx + 10, py = by - 8;
                g2.drawRect(px, py, 8, 10);
                g2.setColor(UIConstants.PRIMARY_BLUE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine(px + 2, py + 5, px + 3, py + 7);
                g2.drawLine(px + 3, py + 7, px + 6, py + 3);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}
