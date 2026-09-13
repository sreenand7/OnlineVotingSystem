package com.voting.gui;

import com.voting.model.Candidate;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.Collection;

/**
 * Screen 7 — Candidates list.
 *
 * <p>JTable showing ID / Candidate / Party for the current election.
 * Reachable from both Election Management and the Voting screen;
 * the back button returns to whichever screen opened it.</p>
 */
public class CandidatesPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private DefaultTableModel    tableModel;
    private String               returnScreen = VotingAppFrame.SCREEN_ELECTION_MGMT;

    public CandidatesPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    /** Sets the screen the back button should navigate to. */
    public void setReturnScreen(String screen) {
        this.returnScreen = screen;
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel("Candidates", "← Back",
                () -> frame.showScreen(returnScreen));
        add(header, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Candidate", "Party"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);

        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        if (frame.getCurrentManager() == null) return;

        try {
            Collection<Candidate> candidates =
                    frame.getCurrentManager().getCandidates();
            for (Candidate c : candidates) {
                tableModel.addRow(new Object[]{
                        c.getCandidateId(), c.getName(),
                        c.getPoliticalParty()
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error loading candidates: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
