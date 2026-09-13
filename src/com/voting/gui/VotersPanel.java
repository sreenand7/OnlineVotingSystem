package com.voting.gui;

import com.voting.model.Vote;
import com.voting.model.Voter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * Screen 8 — Voters list.
 *
 * <p>JTable showing ID / Voter / Status for the current election.
 * Voted status is determined by cross-referencing
 * {@code manager.getVotes()} — no direct DAO call needed.</p>
 */
public class VotersPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private DefaultTableModel    tableModel;
    private JTable               table;
    private String               returnScreen = VotingAppFrame.SCREEN_ELECTION_MGMT;

    public VotersPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    public void setReturnScreen(String screen) {
        this.returnScreen = screen;
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel("Voters", "← Back",
                () -> frame.showScreen(returnScreen));
        add(header, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Voter", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);

        // Color-code the Status column
        table.getColumnModel().getColumn(2).setCellRenderer(
                new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                String status = val != null ? val.toString() : "";
                if (status.startsWith("Voted")) {
                    setForeground(UIConstants.ACCENT_GREEN);
                } else {
                    setForeground(UIConstants.TEXT_SECONDARY);
                }
                if (!sel) {
                    setBackground(row % 2 == 0
                            ? UIConstants.BG_DARK : UIConstants.TABLE_ROW_ALT);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });

        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        if (frame.getCurrentManager() == null) return;

        try {
            // Build set of voter IDs who have voted (via VotingManager)
            List<Vote> votes = frame.getCurrentManager().getVotes();
            Set<String> votedIds = new HashSet<>();
            for (Vote v : votes) {
                votedIds.add(v.getVoter().getVoterId());
            }

            Collection<Voter> voters = frame.getCurrentManager().getVoters();
            for (Voter v : voters) {
                String status = votedIds.contains(v.getVoterId())
                        ? "Voted \u2713" : "Not Voted";
                tableModel.addRow(new Object[]{
                        v.getVoterId(), v.getName(), status
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error loading voters: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
