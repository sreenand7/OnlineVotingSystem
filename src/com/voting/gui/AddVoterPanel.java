package com.voting.gui;

import com.voting.model.Voter;
import com.voting.util.VotingException;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/**
 * Screen 6 — Add Voter form.
 *
 * <p>Two fields (ID, name) and two buttons (Add, Cancel).
 * Uses {@code manager.registerVoter(new Voter(id, name))} from the
 * backend.</p>
 */
public class AddVoterPanel extends JPanel {

    private final VotingAppFrame frame;
    private JTextField idField;
    private JTextField nameField;

    public AddVoterPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel header = new JLabel("Add Voter");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(32));

        card.add(UIConstants.createFieldLabel("Voter ID"));
        card.add(Box.createVerticalStrut(8));
        idField = UIConstants.createStyledField();
        idField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(idField);
        card.add(Box.createVerticalStrut(20));

        card.add(UIConstants.createFieldLabel("Voter Name"));
        card.add(Box.createVerticalStrut(8));
        nameField = UIConstants.createStyledField();
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameField);
        card.add(Box.createVerticalStrut(32));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton addBtn = UIConstants.createPrimaryButton("Add Voter");
        addBtn.setPreferredSize(new Dimension(160, 44));
        addBtn.addActionListener(e -> addVoter());
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

    private void addVoter() {
        String id   = idField.getText().trim();
        String name = nameField.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Both voter ID and name are required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validate numeric ID
        try {
            int parsed = Integer.parseInt(id);
            if (parsed <= 0) {
                JOptionPane.showMessageDialog(frame,
                        "Voter ID must be a positive number.",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame,
                    "Voter ID must be a numeric value.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            frame.getCurrentManager().registerVoter(new Voter(id, name));
            JOptionPane.showMessageDialog(frame,
                    "Voter '" + name + "' registered successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException | VotingException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
    }
}
