package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Screen 4 — Election Management hub.
 *
 * <p>Shows election name, status, and a 3×2 grid of action buttons:
 * Add Candidate, Add Voter, View Candidates, View Voters,
 * Start Election, Back.</p>
 */
public class ElectionManagementPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private JLabel  nameLabel;
    private JLabel  statusLabel;
    private JButton startBtn;

    public ElectionManagementPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    private void buildUI() {
        // ── Header ──────────────────────────────────────────────────────
        JPanel header = UIConstants.createHeaderPanel(
                "Election Management", "← Back to Home",
                () -> frame.showScreen(VotingAppFrame.SCREEN_HOME));
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
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        nameLabel = new JLabel("Election Name");
        nameLabel.setFont(UIConstants.FONT_HEADER);
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(nameLabel);
        infoCard.add(Box.createVerticalStrut(8));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.setOpaque(false);
        JLabel statusText = new JLabel("Status:");
        statusText.setFont(UIConstants.FONT_BODY);
        statusText.setForeground(UIConstants.TEXT_SECONDARY);
        statusRow.add(statusText);

        statusLabel = new JLabel("UPCOMING");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.STATUS_UPCOMING);
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

        JButton addCandBtn = UIConstants.createSecondaryButton("Add Candidate");
        addCandBtn.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_ADD_CANDIDATE));
        grid.add(addCandBtn);

        JButton addVoterBtn = UIConstants.createSecondaryButton("Add Voter");
        addVoterBtn.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_ADD_VOTER));
        grid.add(addVoterBtn);

        JButton viewCandBtn = UIConstants.createSecondaryButton("View Candidates");
        viewCandBtn.addActionListener(e -> {
            CandidatesPanel cp = frame.getScreen(VotingAppFrame.SCREEN_CANDIDATES);
            cp.setReturnScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
            frame.showScreen(VotingAppFrame.SCREEN_CANDIDATES);
        });
        grid.add(viewCandBtn);

        JButton viewVoterBtn = UIConstants.createSecondaryButton("View Voters");
        viewVoterBtn.addActionListener(e -> {
            VotersPanel vp = frame.getScreen(VotingAppFrame.SCREEN_VOTERS);
            vp.setReturnScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
            frame.showScreen(VotingAppFrame.SCREEN_VOTERS);
        });
        grid.add(viewVoterBtn);

        startBtn = UIConstants.createPrimaryButton("Start Election");
        startBtn.addActionListener(e -> startElection());
        grid.add(startBtn);

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_ELECTION_SELECT));
        grid.add(backBtn);

        center.add(grid);
        add(center, BorderLayout.CENTER);
    }

    // ── Start Election ──────────────────────────────────────────────────

    private void startElection() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            if (manager.getCandidates().isEmpty()) {
                warn("Register at least one candidate before starting.");
                return;
            }
            if (manager.getVoters().isEmpty()) {
                warn("Register at least one voter before starting.");
                return;
            }

            Object[] options = {"Cancel", "OK"};
            int confirm = JOptionPane.showOptionDialog(frame,
                    "Are you sure you want to start the election?",
                    "Start Election",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[1]);
            if (confirm != 1) return;

            if (manager.getDurationMinutes() <= 0) {
                String input = JOptionPane.showInputDialog(frame,
                        "Enter voting duration in minutes:",
                        "Duration Required",
                        JOptionPane.QUESTION_MESSAGE);
                if (input == null) return;
                int minutes = Integer.parseInt(input.trim());
                manager.startElection(minutes);
            } else {
                manager.startElection();
            }

            frame.setCurrentRecord(
                    new ElectionDAO().findById(manager.getElectionId()));
            frame.showScreen(VotingAppFrame.SCREEN_VOTING);

        } catch (NumberFormatException ex) {
            warn("Please enter a valid number.");
        } catch (IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg,
                "Cannot Start", JOptionPane.WARNING_MESSAGE);
    }

    // ── Refreshable ─────────────────────────────────────────────────────

    @Override
    public void refresh() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            nameLabel.setText(manager.getElectionName());
            ElectionDAO.ElectionRecord record =
                    new ElectionDAO().findById(manager.getElectionId());
            if (record != null) {
                frame.setCurrentRecord(record);
                statusLabel.setText(record.status);
                switch (record.status) {
                    case "UPCOMING":
                        statusLabel.setForeground(UIConstants.STATUS_UPCOMING);
                        startBtn.setEnabled(true);
                        break;
                    case "ACTIVE":
                        statusLabel.setForeground(UIConstants.STATUS_ACTIVE);
                        startBtn.setEnabled(false);
                        break;
                    default:
                        statusLabel.setForeground(UIConstants.STATUS_COMPLETED);
                        startBtn.setEnabled(false);
                }
            }
        } catch (SQLException ex) {
            nameLabel.setText("Error loading election");
        }
    }
}
