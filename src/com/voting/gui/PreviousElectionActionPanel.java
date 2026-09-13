package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Action screen for a selected previous election.
 *
 * <p>Shows election name, ID, status, and action buttons:
 * View Results, View Candidates, View Details, Export Results,
 * Conduct Re-election, Back.</p>
 */
public class PreviousElectionActionPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private JLabel  nameLabel;
    private JLabel  idLabel;
    private JLabel  statusLabel;

    public PreviousElectionActionPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    private void buildUI() {
        // ── Header ──────────────────────────────────────────────────────
        JPanel header = UIConstants.createHeaderPanel(
                "Selected Election", "← Back",
                () -> frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS));
        add(header, BorderLayout.NORTH);

        // ── Center ──────────────────────────────────────────────────────
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Info card
        JPanel infoCard = new UIConstants.RoundedPanel(12, UIConstants.BG_CARD);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        nameLabel = new JLabel("Election Name");
        nameLabel.setFont(UIConstants.FONT_HEADER);
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(nameLabel);
        infoCard.add(Box.createVerticalStrut(8));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.setOpaque(false);
        
        idLabel = new JLabel("ID: ");
        idLabel.setFont(UIConstants.FONT_BODY);
        idLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusRow.add(idLabel);
        
        statusRow.add(Box.createHorizontalStrut(16));

        JLabel statusText = new JLabel("Status:");
        statusText.setFont(UIConstants.FONT_BODY);
        statusText.setForeground(UIConstants.TEXT_SECONDARY);
        statusRow.add(statusText);

        statusLabel = new JLabel("COMPLETED");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusRow.add(statusLabel);

        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(statusRow);

        center.add(infoCard);
        center.add(Box.createVerticalStrut(32));

        // Action grid (3 rows × 2 cols)
        JPanel grid = new JPanel(new GridLayout(3, 2, 16, 16));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JButton viewResultsBtn = UIConstants.createPrimaryButton("View Results");
        viewResultsBtn.addActionListener(e -> {
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTION_ACTION);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        });
        grid.add(viewResultsBtn);

        JButton viewCandBtn = UIConstants.createSecondaryButton("View Candidates");
        viewCandBtn.addActionListener(e -> {
            CandidatesPanel cp = frame.getScreen(VotingAppFrame.SCREEN_CANDIDATES);
            cp.setReturnScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTION_ACTION);
            frame.showScreen(VotingAppFrame.SCREEN_CANDIDATES);
        });
        grid.add(viewCandBtn);

        JButton viewDetailsBtn = UIConstants.createSecondaryButton("View Details");
        viewDetailsBtn.addActionListener(e -> {
            ElectionDetailsPanel dp = frame.getScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
            dp.setReturnScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTION_ACTION);
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
        });
        grid.add(viewDetailsBtn);

        JButton exportResultsBtn = UIConstants.createSecondaryButton("Export Results");
        exportResultsBtn.addActionListener(e -> exportSelection());
        grid.add(exportResultsBtn);

        JButton reElectBtn = UIConstants.createSecondaryButton("Conduct Re-election");
        reElectBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_RE_ELECTION));
        grid.add(reElectBtn);

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS));
        grid.add(backBtn);

        center.add(grid);
        add(center, BorderLayout.CENTER);
    }
    
    private void exportSelection() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        
        try {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("election_results_" + manager.getElectionId() + ".txt"));
            int result = fc.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                manager.exportResults(fc.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(frame,
                        "Results exported successfully!",
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException | IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error exporting results: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Refreshable ─────────────────────────────────────────────────────

    @Override
    public void refresh() {
        ElectionDAO.ElectionRecord record = frame.getCurrentRecord();
        if (record == null) return;

        nameLabel.setText(record.name);
        idLabel.setText("ID: " + record.electionId);
        statusLabel.setText(record.status);
        
        switch (record.status) {
            case "UPCOMING":
                statusLabel.setForeground(UIConstants.STATUS_UPCOMING);
                break;
            case "ACTIVE":
                statusLabel.setForeground(UIConstants.STATUS_ACTIVE);
                break;
            default:
                statusLabel.setForeground(UIConstants.STATUS_COMPLETED);
                break;
        }
    }
}
