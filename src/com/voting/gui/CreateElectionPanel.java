package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Screen 3 — Create New Election.
 *
 * <p>Simple form with election name and duration fields.
 * Creates the election via {@link VotingManager} and navigates to
 * Election Management on success.</p>
 */
public class CreateElectionPanel extends JPanel {

    private final VotingAppFrame frame;
    private JTextField nameField;
    private JTextField durationField;

    public CreateElectionPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        // Header
        JLabel header = new JLabel("Create New Election");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(32));

        // Election Name
        card.add(UIConstants.createFieldLabel("Election Name"));
        card.add(Box.createVerticalStrut(8));
        nameField = UIConstants.createStyledField();
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameField);
        card.add(Box.createVerticalStrut(20));

        // Duration
        card.add(UIConstants.createFieldLabel("Duration (minutes)"));
        card.add(Box.createVerticalStrut(8));
        durationField = UIConstants.createStyledField();
        durationField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(durationField);
        card.add(Box.createVerticalStrut(8));

        card.add(UIConstants.createMutedLabel(
                "Voting closes automatically once this duration elapses."));
        card.add(Box.createVerticalStrut(32));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createBtn = UIConstants.createPrimaryButton("Create Election");
        createBtn.setPreferredSize(new Dimension(180, 44));
        createBtn.addActionListener(e -> createElection());
        JButton cancelBtn = UIConstants.createSecondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 44));
        cancelBtn.addActionListener(e -> {
            clearFields();
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_SELECT);
        });
        btnRow.add(cancelBtn);

        btnRow.add(createBtn);

        card.add(btnRow);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    // ── Actions ─────────────────────────────────────────────────────────

    private void createElection() {
        String name = nameField.getText().trim();
        String durStr = durationField.getText().trim();

        if (name.isEmpty()) {
            warn("Election name cannot be empty.");
            return;
        }
        int duration;
        try {
            duration = Integer.parseInt(durStr);
            if (duration <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            warn("Please enter a valid positive number for duration.");
            return;
        }

        try {
            if (new ElectionDAO().findByName(name) != null) {
                warn("An election with this name already exists.");
                return;
            }

            VotingManager manager = new VotingManager(name, duration);
            frame.setCurrentManager(manager);
            frame.setCurrentRecord(
                    new ElectionDAO().findById(manager.getElectionId()));
            clearFields();

            JOptionPane.showMessageDialog(frame,
                    String.format("Election '%s' created successfully! (ID: %d)",
                            name, manager.getElectionId()),
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        nameField.setText("");
        durationField.setText("");
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg,
                "Validation Error", JOptionPane.WARNING_MESSAGE);
    }
}
