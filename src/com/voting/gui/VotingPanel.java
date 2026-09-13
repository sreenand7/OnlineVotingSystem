package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;
import com.voting.model.Candidate;
import com.voting.util.VotingException;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Screen 9 — Voting screen.
 *
 * <p>Shows the election header, live stats (status / time remaining /
 * votes cast), a candidate table with per-row Vote buttons, and
 * bottom action buttons. A {@link javax.swing.Timer} counts down
 * using the real election end time from the backend.</p>
 */
public class VotingPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private final ElectionDAO    electionDAO = new ElectionDAO();

    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            electionNameLabel;
    private JLabel            statusValueLabel;
    private JLabel            timeRemainingLabel;
    private JLabel            votesCastLabel;
    private Timer             countdownTimer;

    public VotingPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        buildUI();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UI Construction
    // ═══════════════════════════════════════════════════════════════════

    private void buildUI() {
        // ── Top section: header + stats ─────────────────────────────────
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Header row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        electionNameLabel = new JLabel("Election — Voting");
        electionNameLabel.setFont(UIConstants.FONT_HEADER);
        electionNameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        headerRow.add(electionNameLabel, BorderLayout.WEST);

        JLabel openBadge = new JLabel("  OPEN  ");
        openBadge.setFont(UIConstants.FONT_SMALL);
        openBadge.setForeground(UIConstants.ACCENT_GREEN);
        openBadge.setOpaque(true);
        openBadge.setBackground(new Color(0x12, 0x2E, 0x1A));
        openBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        headerRow.add(openBadge, BorderLayout.EAST);

        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(headerRow);
        top.add(Box.createVerticalStrut(20));

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        statusValueLabel   = new JLabel("Voting in progress");
        timeRemainingLabel = new JLabel("--:--");
        votesCastLabel     = new JLabel("0 / 0");

        statsRow.add(createStatCard("Status",         statusValueLabel));
        statsRow.add(createStatCard("Time Remaining", timeRemainingLabel));
        statsRow.add(createStatCard("Votes Cast",     votesCastLabel));

        top.add(statsRow);
        top.add(Box.createVerticalStrut(20));

        add(top, BorderLayout.NORTH);

        // ── Center: candidate table ────────────────────────────────────
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Candidate", "Party", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3;            // only the Vote button column
            }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        TableColumn actionCol = table.getColumnModel().getColumn(3);
        actionCol.setPreferredWidth(100);
        actionCol.setMaxWidth(120);
        actionCol.setCellRenderer(new VoteButtonRenderer());
        actionCol.setCellEditor(new VoteButtonEditor());

        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);

        // ── Bottom buttons ─────────────────────────────────────────────
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftNav.setOpaque(false);

        JButton viewCandBtn = UIConstants.createSecondaryButton("View Candidates");
        viewCandBtn.setPreferredSize(new Dimension(160, 40));
        viewCandBtn.addActionListener(e -> {
            CandidatesPanel cp = frame.getScreen(VotingAppFrame.SCREEN_CANDIDATES);
            cp.setReturnScreen(VotingAppFrame.SCREEN_VOTING);
            frame.showScreen(VotingAppFrame.SCREEN_CANDIDATES);
        });
        leftNav.add(viewCandBtn);

        JButton viewVoterBtn = UIConstants.createSecondaryButton("View Voters");
        viewVoterBtn.setPreferredSize(new Dimension(140, 40));
        viewVoterBtn.addActionListener(e -> {
            VotersPanel vp = frame.getScreen(VotingAppFrame.SCREEN_VOTERS);
            vp.setReturnScreen(VotingAppFrame.SCREEN_VOTING);
            frame.showScreen(VotingAppFrame.SCREEN_VOTERS);
        });
        leftNav.add(viewVoterBtn);

        JButton liveResultsBtn = UIConstants.createSecondaryButton("Live Results");
        liveResultsBtn.setPreferredSize(new Dimension(140, 40));
        liveResultsBtn.addActionListener(e -> {
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_VOTING);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        });
        leftNav.add(liveResultsBtn);
        
        bottom.add(leftNav, BorderLayout.WEST);

        JPanel rightAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightAction.setOpaque(false);
        JButton endBtn = UIConstants.createDangerButton("End Voting");
        endBtn.setPreferredSize(new Dimension(140, 40));
        endBtn.addActionListener(e -> endVoting());
        rightAction.add(endBtn);
        
        bottom.add(rightAction, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
    }

    // ── Stat card helper ────────────────────────────────────────────────

    private JPanel createStatCard(String label, JLabel valueLabel) {
        JPanel card = new UIConstants.RoundedPanel(8, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_STAT_LABEL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbl);
        card.add(Box.createVerticalStrut(4));

        valueLabel.setFont(UIConstants.FONT_STAT_VALUE);
        valueLabel.setForeground(UIConstants.TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(valueLabel);

        return card;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Actions
    // ═══════════════════════════════════════════════════════════════════

    private void handleVote(String candidateId) {
        String voterId = JOptionPane.showInputDialog(frame,
                "Enter your Voter ID:",
                "Cast Vote", JOptionPane.PLAIN_MESSAGE);
        if (voterId == null || voterId.trim().isEmpty()) return;

        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            manager.castVote(voterId.trim(), candidateId);
            JOptionPane.showMessageDialog(frame,
                    "Vote submitted successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshStats();
        } catch (VotingException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Vote Rejected", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void endVoting() {
        Object[] options = {"Cancel", "OK"};
        int confirm = JOptionPane.showOptionDialog(frame,
                "Are you sure you want to end voting?",
                "End Voting",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);
        if (confirm != 1) return;

        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            electionDAO.updateStatus(manager.getElectionId(),
                    ElectionDAO.Status.COMPLETED);
            stopTimer();
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_HOME);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error ending voting: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Timer
    // ═══════════════════════════════════════════════════════════════════

    private void startTimer() {
        stopTimer();
        countdownTimer = new Timer(1000, e -> updateCountdown());
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void updateCountdown() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) { stopTimer(); return; }

        try {
            LocalDateTime end = manager.getElectionEnd();
            Duration remaining = Duration.between(LocalDateTime.now(), end);

            if (remaining.isNegative() || remaining.isZero()) {
                stopTimer();
                timeRemainingLabel.setText("00:00");
                statusValueLabel.setText("Voting ended");
                electionDAO.updateStatus(manager.getElectionId(),
                        ElectionDAO.Status.COMPLETED);

                JOptionPane.showMessageDialog(frame,
                        "Election voting window has ended.",
                        "Time's Up", JOptionPane.INFORMATION_MESSAGE);

                ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
                rp.setReturnScreen(VotingAppFrame.SCREEN_HOME);
                frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
                return;
            }

            long totalSecs = remaining.getSeconds();
            timeRemainingLabel.setText(
                    String.format("%02d:%02d", totalSecs / 60, totalSecs % 60));
            refreshStats();

        } catch (SQLException ex) {
            timeRemainingLabel.setText("Error");
        }
    }

    private void refreshStats() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            int votesCast  = manager.getVotes().size();
            int totalVoters = manager.getVoters().size();
            votesCastLabel.setText(votesCast + " / " + totalVoters);
        } catch (SQLException ignored) { }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Refreshable
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void refresh() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            electionNameLabel.setText(
                    manager.getElectionName() + " — Voting");

            tableModel.setRowCount(0);
            Collection<Candidate> candidates = manager.getCandidates();
            for (Candidate c : candidates) {
                tableModel.addRow(new Object[]{
                        c.getCandidateId(), c.getName(),
                        c.getPoliticalParty(), "Vote"
                });
            }

            statusValueLabel.setText("Voting in progress");
            refreshStats();
            startTimer();

        } catch (SQLException ex) {
            electionNameLabel.setText("Error loading election");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Vote Button (table cell renderer / editor)
    // ═══════════════════════════════════════════════════════════════════

    /** Renders a blue "Vote" label in every Action cell. */
    private class VoteButtonRenderer extends JLabel
            implements TableCellRenderer {

        VoteButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setBackground(UIConstants.PRIMARY_BLUE);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            setText("Vote");
            return this;
        }
    }

    /** Clickable editor that prompts for voter ID and casts the vote. */
    private class VoteButtonEditor extends AbstractCellEditor
            implements TableCellEditor {

        private final JButton button;

        VoteButtonEditor() {
            button = new JButton("Vote");
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(UIConstants.PRIMARY_BLUE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                int row = table.getEditingRow();
                fireEditingStopped();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    SwingUtilities.invokeLater(() -> {
                        String candidateId =
                                (String) tableModel.getValueAt(row, 0);
                        handleVote(candidateId);
                    });
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Vote";
        }
    }
}
