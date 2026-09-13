package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Screen 2 — Start / Select Election.
 *
 * <p>Three centered buttons: Create New, Enter Existing ID, Back.</p>
 */
public class ElectionSelectionPanel extends JPanel {

    private final VotingAppFrame frame;

    public ElectionSelectionPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel header = new JLabel("Start / Select Election");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(40));

        JButton btnCreate = UIConstants.createPrimaryButton("Create New Election");
        btnCreate.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_CREATE_ELECTION));
        card.add(UIConstants.wrapButton(btnCreate, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnExisting = UIConstants.createSecondaryButton("Enter Existing Election ID");
        btnExisting.addActionListener(e -> enterExistingElection());
        card.add(UIConstants.wrapButton(btnExisting, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnBack = UIConstants.createSecondaryButton("Back to Home");
        btnBack.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_HOME));
        card.add(UIConstants.wrapButton(btnBack, 48));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    // ── Enter Existing Election ─────────────────────────────────────────

    private void enterExistingElection() {
        String input = JOptionPane.showInputDialog(frame,
                "Enter Election ID:",
                "Existing Election", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int electionId = Integer.parseInt(input.trim());
            VotingManager manager = new VotingManager(electionId);
            ElectionDAO.ElectionRecord record =
                    new ElectionDAO().findById(electionId);
            if (record == null) {
                showError("Election not found.");
                return;
            }

            frame.setCurrentManager(manager);
            frame.setCurrentRecord(record);

            switch (record.status) {
                case "UPCOMING":
                    frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
                    break;
                case "ACTIVE":
                    frame.showScreen(VotingAppFrame.SCREEN_VOTING);
                    break;
                case "COMPLETED":
                    frame.showScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
                    break;
                default:
                    showError("Unknown election status: " + record.status);
            }
        } catch (NumberFormatException ex) {
            showError("Please enter a valid numeric ID.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
