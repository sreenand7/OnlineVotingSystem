package com.voting.gui;

import com.voting.model.Candidate;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Screen 5 — Add Candidate form.
 *
 * <p>Two fields (name, party) and two buttons (Add, Cancel).
 * Uses {@code manager.registerCandidate(name, party)} from the backend.</p>
 */
public class AddCandidatePanel extends JPanel {

    private final VotingAppFrame frame;
    private JTextField nameField;
    private JTextField partyField;

    public AddCandidatePanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel header = new JLabel("Add Candidate");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(32));

        card.add(UIConstants.createFieldLabel("Candidate Name"));
        card.add(Box.createVerticalStrut(8));
        nameField = UIConstants.createStyledField();
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameField);
        card.add(Box.createVerticalStrut(20));

        card.add(UIConstants.createFieldLabel("Political Party"));
        card.add(Box.createVerticalStrut(8));
        partyField = UIConstants.createStyledField();
        partyField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(partyField);
        card.add(Box.createVerticalStrut(32));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addBtn = UIConstants.createPrimaryButton("Add Candidate");
        addBtn.setPreferredSize(new Dimension(180, 44));
        addBtn.addActionListener(e -> addCandidate());
        JButton cancelBtn = UIConstants.createSecondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 44));
        cancelBtn.addActionListener(e -> {
            clearFields();
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
        });
        btnRow.add(cancelBtn);

        btnRow.add(addBtn);

        card.add(btnRow);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    private void addCandidate() {
        String name  = nameField.getText().trim();
        String party = partyField.getText().trim();

        if (name.isEmpty() || party.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Both candidate name and political party are required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Candidate c = frame.getCurrentManager().registerCandidate(name, party);
            JOptionPane.showMessageDialog(frame,
                    String.format("Candidate '%s' registered! (ID: %s)",
                            name, c.getCandidateId()),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        nameField.setText("");
        partyField.setText("");
    }
}
