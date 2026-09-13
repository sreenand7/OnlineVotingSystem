package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Screen 13 — Re-election form.
 *
 * <p>Creates a new election linked to the current one.
 * Uses {@code new VotingManager(name, duration, parentId)}.</p>
 */
public class ReElectionPanel extends JPanel implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private JTextField nameField;
    private JTextField durationField;

    public ReElectionPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel header = new JLabel("Conduct Re-election");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(32));

        card.add(UIConstants.createFieldLabel("New Election Name"));
        card.add(Box.createVerticalStrut(8));
        nameField = UIConstants.createStyledField();
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameField);
        card.add(Box.createVerticalStrut(20));

        card.add(UIConstants.createFieldLabel("Voting Duration (minutes)"));
        card.add(Box.createVerticalStrut(8));
        durationField = UIConstants.createStyledField();
        durationField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(durationField);
        card.add(Box.createVerticalStrut(32));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton createBtn = UIConstants.createPrimaryButton("Create Re-election");
        createBtn.setPreferredSize(new Dimension(180, 44));
        createBtn.addActionListener(e -> createReElection());
        JButton cancelBtn = UIConstants.createSecondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 44));
        cancelBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS));
        btnRow.add(cancelBtn);

        btnRow.add(createBtn);

        card.add(btnRow);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    private void createReElection() {
        ElectionDAO.ElectionRecord parentRecord = frame.getCurrentRecord();
        if (parentRecord == null) return;

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

            VotingManager manager = new VotingManager(name, duration, parentRecord.electionId);
            frame.setCurrentManager(manager);
            frame.setCurrentRecord(new ElectionDAO().findById(manager.getElectionId()));

            JOptionPane.showMessageDialog(frame,
                    String.format("Re-election '%s' created successfully! (ID: %d)",
                            name, manager.getElectionId()),
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void refresh() {
        ElectionDAO.ElectionRecord record = frame.getCurrentRecord();
        if (record != null) {
            nameField.setText(record.name + " (Re-election)");
        }
        durationField.setText("");
    }
}
