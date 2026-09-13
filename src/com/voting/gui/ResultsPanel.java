package com.voting.gui;

import com.voting.manager.VotingManager;
import com.voting.model.Candidate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Screen 10 — Election Results.
 *
 * <p>Shows a result table (ID / Candidate / Party / Votes), summary
 * statistics (total votes, registered voters, turnout, winner), and
 * export / back buttons.  Reachable from the Voting screen (live
 * results) or from Previous Elections.</p>
 */
public class ResultsPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private DefaultTableModel    tableModel;
    private JLabel totalVotesLabel;
    private JLabel registeredVotersLabel;
    private JLabel turnoutLabel;
    private JLabel winnerLabel;
    private String returnScreen = VotingAppFrame.SCREEN_HOME;

    public ResultsPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        buildUI();
    }

    public void setReturnScreen(String screen) {
        this.returnScreen = screen;
    }

    private void buildUI() {
        // Header
        JPanel header = UIConstants.createHeaderPanel(
                "Election Results", null, null);
        add(header, BorderLayout.NORTH);

        // Center
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);

        // Table
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Candidate", "Party", "Votes"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        center.add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setPreferredSize(new Dimension(0, 70));

        totalVotesLabel       = new JLabel("0");
        registeredVotersLabel = new JLabel("0");
        turnoutLabel          = new JLabel("0%");
        winnerLabel           = new JLabel("-");

        statsRow.add(createStatCard("Total Votes",       totalVotesLabel));
        statsRow.add(createStatCard("Registered Voters", registeredVotersLabel));
        statsRow.add(createStatCard("Turnout",           turnoutLabel));
        statsRow.add(createStatCard("Winner",            winnerLabel));

        center.add(statsRow, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // Bottom buttons
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.setPreferredSize(new Dimension(100, 40));
        backBtn.addActionListener(e -> frame.showScreen(returnScreen));
        bottom.add(backBtn, BorderLayout.WEST);

        JButton exportBtn = UIConstants.createPrimaryButton("Export Results");
        exportBtn.setPreferredSize(new Dimension(160, 40));
        exportBtn.addActionListener(e -> exportResults());
        bottom.add(exportBtn, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String label, JLabel valueLabel) {
        JPanel card = new UIConstants.RoundedPanel(8, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

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

    // ── Export ───────────────────────────────────────────────────────────

    private void exportResults() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("election_results.txt"));
        int result = fc.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;

        try {
            manager.exportResults(fc.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(frame,
                    "Results exported successfully!",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Refreshable ─────────────────────────────────────────────────────

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            Map<String, Long> tally = manager.getTallyMap();
            Map<String, Candidate> candidates = new LinkedHashMap<>();
            for (Candidate c : manager.getCandidates()) {
                candidates.put(c.getCandidateId(), c);
            }

            long totalVotes  = tally.values().stream()
                    .mapToLong(Long::longValue).sum();
            int  totalVoters = manager.getVoters().size();

            for (Map.Entry<String, Long> entry : tally.entrySet()) {
                Candidate c = candidates.get(entry.getKey());
                if (c != null) {
                    tableModel.addRow(new Object[]{
                            c.getCandidateId(), c.getName(),
                            c.getPoliticalParty(), entry.getValue()
                    });
                }
            }

            totalVotesLabel.setText(String.valueOf(totalVotes));
            registeredVotersLabel.setText(String.valueOf(totalVoters));
            turnoutLabel.setText(totalVoters == 0 ? "N/A"
                    : String.format("%.1f%%",
                            100.0 * totalVotes / totalVoters));

            // Determine winner
            if (!tally.isEmpty()) {
                long maxVotes = tally.values().iterator().next();
                List<String> winners = tally.entrySet().stream()
                        .filter(e -> e.getValue() == maxVotes)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                if (winners.size() == 1) {
                    Candidate w = candidates.get(winners.get(0));
                    winnerLabel.setText(w != null ? w.getName() : "-");
                } else {
                    String tied = winners.stream()
                            .map(id -> candidates.containsKey(id)
                                    ? candidates.get(id).getName() : id)
                            .collect(Collectors.joining(", "));
                    winnerLabel.setText("TIE: " + tied);
                }
            } else {
                winnerLabel.setText("-");
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error loading results: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
