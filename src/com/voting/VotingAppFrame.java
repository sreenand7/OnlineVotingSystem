package com.voting;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

class AddCandidatePanel extends JPanel {

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

class AddVoterPanel extends JPanel {

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

class CandidatesPanel extends JPanel
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

class CreateElectionPanel extends JPanel {

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

class ElectionDetailsPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private JLabel detailsLabel;
    private JLabel candidatesCountLabel;
    private JLabel votersCountLabel;
    private JLabel votesCastLabel;
    private String returnScreen = VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS;
    
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ElectionDetailsPanel(VotingAppFrame frame) {
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
        JPanel header = UIConstants.createHeaderPanel(
                "Election Details", "← Back",
                () -> frame.showScreen(returnScreen));
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel infoCard = new UIConstants.RoundedPanel(12, UIConstants.BG_CARD);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        detailsLabel = new JLabel("<html>Loading details...</html>");
        detailsLabel.setFont(UIConstants.FONT_BODY);
        detailsLabel.setForeground(UIConstants.TEXT_PRIMARY);
        detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(detailsLabel);
        
        center.add(infoCard);
        center.add(Box.createVerticalStrut(32));

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        candidatesCountLabel = new JLabel("0");
        votersCountLabel     = new JLabel("0");
        votesCastLabel       = new JLabel("0");

        statsRow.add(createStatCard("Candidates", candidatesCountLabel));
        statsRow.add(createStatCard("Registered Voters", votersCountLabel));
        statsRow.add(createStatCard("Total Votes Cast", votesCastLabel));

        center.add(statsRow);
        add(center, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String label, JLabel valueLabel) {
        JPanel card = new UIConstants.RoundedPanel(8, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

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

    @Override
    public void refresh() {
        ElectionDAO.ElectionRecord record = frame.getCurrentRecord();
        VotingManager manager = frame.getCurrentManager();
        if (record == null || manager == null) return;

        String parentText = record.parentElectionId != null 
                ? " (Re-election of ID " + record.parentElectionId + ")" 
                : "";

        StringBuilder sb = new StringBuilder("<html>");
        sb.append("<h2 style='margin-top:0;'>").append(record.name).append("</h2>");
        sb.append("<b>Election ID:</b> ").append(record.electionId).append(parentText).append("<br><br>");
        sb.append("<b>Status:</b> ").append(record.status).append("<br>");
        sb.append("<b>Start Time:</b> ").append(record.startTime != null ? record.startTime.format(FMT) : "N/A").append("<br>");
        sb.append("<b>End Time:</b> ").append(record.endTime != null ? record.endTime.format(FMT) : "N/A").append("<br>");
        sb.append("</html>");
        detailsLabel.setText(sb.toString());

        try {
            candidatesCountLabel.setText(String.valueOf(manager.getCandidates().size()));
            votersCountLabel.setText(String.valueOf(manager.getVoters().size()));
            votesCastLabel.setText(String.valueOf(manager.getVotes().size()));
        } catch (SQLException ex) {
            candidatesCountLabel.setText("Error");
            votersCountLabel.setText("Error");
            votesCastLabel.setText("Error");
        }
    }
}

class ElectionManagementPanel extends JPanel
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

class ElectionSelectionPanel extends JPanel {

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

class HomePanel extends JPanel {

    private final VotingAppFrame frame;

    public HomePanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        // ── Ballot-box icon (rendered via Java2D) ───────────────────────
        JLabel iconLabel = new JLabel(createBallotIcon());
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(24));

        // ── Title ───────────────────────────────────────────────────────
        JLabel title = new JLabel("Online Voting System");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        // ── Subtitle ────────────────────────────────────────────────────
        JLabel subtitle = new JLabel("Election Management & Voting");
        subtitle.setFont(UIConstants.FONT_SUBTITLE);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(40));

        // ── Buttons ─────────────────────────────────────────────────────
        JButton btnStart = UIConstants.createPrimaryButton("Start / Select Election");
        btnStart.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_ELECTION_SELECT));
        card.add(UIConstants.wrapButton(btnStart, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnHistory = UIConstants.createSecondaryButton("View Previous Elections");
        btnHistory.addActionListener(e ->
                frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS));
        card.add(UIConstants.wrapButton(btnHistory, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnExit = UIConstants.createSecondaryButton("Exit");
        btnExit.addActionListener(e -> { frame.dispose(); System.exit(0); });
        card.add(UIConstants.wrapButton(btnExit, 48));
        card.add(Box.createVerticalStrut(32));

        // ── Footer ──────────────────────────────────────────────────────
        JLabel footer = new JLabel("Secure  |  Fair  |  Transparent");
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(UIConstants.TEXT_MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(footer);

        // Center the card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    // ── Ballot-box icon ─────────────────────────────────────────────────

    private Icon createBallotIcon() {
        final int size = 56;
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 0x20));
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 0x40));
                g2.fillOval(x + 8, y + 8, size - 16, size - 16);
                g2.setColor(UIConstants.PRIMARY_BLUE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                int bx = x + 16, by = y + 18, bw = 24, bh = 20;
                g2.drawRoundRect(bx, by, bw, bh, 3, 3);
                g2.drawLine(bx + 8, by, bx + 16, by);
                g2.drawLine(bx + 8, by - 2, bx + 16, by - 2);
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                int px = bx + 10, py = by - 8;
                g2.drawRect(px, py, 8, 10);
                g2.setColor(UIConstants.PRIMARY_BLUE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.drawLine(px + 2, py + 5, px + 3, py + 7);
                g2.drawLine(px + 3, py + 7, px + 6, py + 3);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}

class PreviousElectionActionPanel extends JPanel
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

class PreviousElectionsPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private DefaultTableModel    tableModel;
    private JTable               table;
    private final ElectionDAO    electionDAO = new ElectionDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JButton selectBtn;

    public PreviousElectionsPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel(
                "Previous Elections", null, null);
        add(header, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Election Name", "End Time", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = table.getSelectedRow() >= 0;
            selectBtn.setEnabled(selected);
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.setPreferredSize(new Dimension(100, 44));
        backBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_HOME));
        bottom.add(backBtn, BorderLayout.WEST);

        selectBtn = UIConstants.createPrimaryButton("Select Election");
        selectBtn.setPreferredSize(new Dimension(180, 44));
        selectBtn.setEnabled(false);
        selectBtn.addActionListener(e -> viewSelection());
        bottom.add(selectBtn, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
    }

    private void viewSelection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Please select an election from the list.",
                    "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int electionId = (int) tableModel.getValueAt(row, 0);
        try {
            ElectionDAO.ElectionRecord record = electionDAO.findById(electionId);
            if (record != null) {
                frame.setCurrentRecord(record);
                frame.setCurrentManager(new VotingManager(electionId));
                frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTION_ACTION);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        try {
            List<ElectionDAO.ElectionRecord> allElections = electionDAO.getAllElections();
            for (ElectionDAO.ElectionRecord rec : allElections) {
                tableModel.addRow(new Object[]{
                        rec.electionId, rec.name,
                        rec.endTime != null ? rec.endTime.format(FMT) : "N/A",
                        rec.status
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading completed elections: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class ReElectionPanel extends JPanel implements VotingAppFrame.Refreshable {

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

class ResultsPanel extends JPanel
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

final class UIConstants {

    private UIConstants() {}

    // ── Colors ──────────────────────────────────────────────────────────

    public static final Color BG_DARK        = new Color(0x12, 0x14, 0x1D);
    public static final Color BG_CARD        = new Color(0x1A, 0x1D, 0x2B);
    public static final Color BG_FIELD       = new Color(0x22, 0x25, 0x36);
    public static final Color PRIMARY_BLUE   = new Color(0x3B, 0x82, 0xF6);
    public static final Color PRIMARY_HOVER  = new Color(0x2B, 0x6C, 0xE0);
    public static final Color BTN_DARK       = new Color(0x1E, 0x21, 0x30);
    public static final Color BTN_DARK_HOVER = new Color(0x28, 0x2C, 0x3E);
    public static final Color BTN_BORDER     = new Color(0x33, 0x37, 0x4D);
    public static final Color TEXT_PRIMARY   = new Color(0xF1, 0xF5, 0xF9);
    public static final Color TEXT_SECONDARY = new Color(0x94, 0xA3, 0xB8);
    public static final Color TEXT_MUTED     = new Color(0x64, 0x74, 0x8B);
    public static final Color ACCENT_GREEN   = new Color(0x22, 0xC5, 0x5E);
    public static final Color DANGER_RED     = new Color(0xEF, 0x44, 0x44);
    public static final Color DANGER_HOVER   = new Color(0xDC, 0x26, 0x26);
    public static final Color STATUS_UPCOMING  = new Color(0xF5, 0x9E, 0x0B);
    public static final Color STATUS_ACTIVE    = ACCENT_GREEN;
    public static final Color STATUS_COMPLETED = TEXT_MUTED;
    public static final Color TABLE_ROW_ALT  = new Color(0x16, 0x18, 0x24);
    public static final Color TABLE_GRID     = new Color(0x2A, 0x2D, 0x3E);

    // ── Fonts ───────────────────────────────────────────────────────────

    public static final Font FONT_TITLE        = new Font("Segoe UI", Font.BOLD,  28);
    public static final Font FONT_HEADER       = new Font("Segoe UI", Font.BOLD,  22);
    public static final Font FONT_SUBTITLE     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BUTTON       = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_BODY         = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL        = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_LABEL        = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font FONT_FIELD        = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD,  12);
    public static final Font FONT_TABLE        = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_STAT_VALUE   = new Font("Segoe UI", Font.BOLD,  18);
    public static final Font FONT_STAT_LABEL   = new Font("Segoe UI", Font.PLAIN, 11);

    // ═══════════════════════════════════════════════════════════════════
    //  Button Factories
    // ═══════════════════════════════════════════════════════════════════

    public static JButton createPrimaryButton(String text) {
        return new StyledButton(text, PRIMARY_BLUE, PRIMARY_HOVER, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return new StyledButton(text, BTN_DARK, BTN_DARK_HOVER, TEXT_PRIMARY);
    }

    public static JButton createDangerButton(String text) {
        return new StyledButton(text, DANGER_RED, DANGER_HOVER, Color.WHITE);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Field / Label Factories
    // ═══════════════════════════════════════════════════════════════════

    /** Creates a dark-themed text field with rounded border. */
    public static JTextField createStyledField() {
        JTextField field = new JTextField();
        field.setFont(FONT_FIELD);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_FIELD);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BTN_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(400, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return field;
    }

    /** Small bold label used above form fields. */
    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** Muted supporting text. */
    public static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Table Factory
    // ═══════════════════════════════════════════════════════════════════

    /** Creates a styled, non-editable JTable with alternating dark rows. */
    public static JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(FONT_TABLE);
        table.setForeground(TEXT_PRIMARY);
        table.setBackground(BG_DARK);
        table.setSelectionBackground(new Color(0x3B, 0x82, 0xF6, 0x30));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setGridColor(TABLE_GRID);
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HEADER);
        header.setForeground(TEXT_SECONDARY);
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TABLE_GRID));
        header.setReorderingAllowed(false);

        // Alternating-row renderer (default for Object.class)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean selected, boolean focused, int row, int col) {
                super.getTableCellRendererComponent(t, val, selected, focused, row, col);
                if (!selected) {
                    setBackground(row % 2 == 0 ? BG_DARK : TABLE_ROW_ALT);
                }
                setForeground(TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });

        return table;
    }

    /** Wraps a JTable in a dark-bordered scroll pane. */
    public static JScrollPane wrapInScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(TABLE_GRID));
        sp.getViewport().setBackground(BG_DARK);
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Header Panel
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a standard screen header with a title on the left
     * and an optional back-link on the right.
     */
    public static JPanel createHeaderPanel(String title,
                                           String backText,
                                           Runnable onBack) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_HEADER);
        titleLabel.setForeground(TEXT_PRIMARY);
        header.add(titleLabel, BorderLayout.WEST);

        if (backText != null && onBack != null) {
            JButton backBtn = new JButton(backText);
            backBtn.setFont(FONT_BODY);
            backBtn.setForeground(TEXT_SECONDARY);
            backBtn.setBorderPainted(false);
            backBtn.setContentAreaFilled(false);
            backBtn.setFocusPainted(false);
            backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            backBtn.addActionListener(e -> onBack.run());
            backBtn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    backBtn.setForeground(TEXT_PRIMARY);
                }
                @Override public void mouseExited(MouseEvent e) {
                    backBtn.setForeground(TEXT_SECONDARY);
                }
            });
            header.add(backBtn, BorderLayout.EAST);
        }

        return header;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Layout Helpers
    // ═══════════════════════════════════════════════════════════════════

    /** Wraps a button in a panel that stretches it horizontally. */
    public static JPanel wrapButton(JButton button, int height) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.add(button, BorderLayout.CENTER);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Reusable Custom Components
    // ═══════════════════════════════════════════════════════════════════

    /**
     * A panel with rounded corners and a solid background color.
     */
    public static class RoundedPanel extends JPanel {
        private final int    radius;
        private final Color  bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius  = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), radius, radius));
            // Subtle border
            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 0x08));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(
                    0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * A styled button with rounded corners and hover color change.
     */
    public static class StyledButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private boolean hovering = false;

        public StyledButton(String text, Color normalBg, Color hoverBg,
                            Color textColor) {
            super(text);
            this.normalBg = normalBg;
            this.hoverBg  = hoverBg;

            setFont(FONT_BUTTON);
            setForeground(textColor);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 44));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    hovering = true;  repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    hovering = false; repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovering ? hoverBg : normalBg);
            g2.fill(new RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), 10, 10));
            // Border for secondary (dark) buttons
            if (normalBg.equals(BTN_DARK)) {
                g2.setColor(BTN_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(
                        0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

class VotersPanel extends JPanel
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

public class VotingAppFrame extends JFrame {

    // ── Screen name constants ───────────────────────────────────────────
    public static final String SCREEN_HOME               = "HOME";
    public static final String SCREEN_ELECTION_SELECT    = "ELECTION_SELECT";
    public static final String SCREEN_CREATE_ELECTION    = "CREATE_ELECTION";
    public static final String SCREEN_ELECTION_MGMT      = "ELECTION_MGMT";
    public static final String SCREEN_ADD_CANDIDATE      = "ADD_CANDIDATE";
    public static final String SCREEN_ADD_VOTER          = "ADD_VOTER";
    public static final String SCREEN_CANDIDATES         = "CANDIDATES";
    public static final String SCREEN_VOTERS             = "VOTERS";
    public static final String SCREEN_VOTING             = "VOTING";
    public static final String SCREEN_RESULTS            = "RESULTS";
    public static final String SCREEN_PREVIOUS_ELECTIONS = "PREVIOUS_ELECTIONS";
    public static final String SCREEN_ELECTION_DETAILS   = "ELECTION_DETAILS";
    public static final String SCREEN_RE_ELECTION        = "RE_ELECTION";
    public static final String SCREEN_PREVIOUS_ELECTION_ACTION = "PREVIOUS_ELECTION_ACTION";

    // ── Layout ──────────────────────────────────────────────────────────
    private final CardLayout            cardLayout;
    private final JPanel                cardPanel;
    private final Map<String, JPanel>   screens = new HashMap<>();

    // ── Shared state ────────────────────────────────────────────────────
    private VotingManager               currentManager;
    private ElectionDAO.ElectionRecord  currentRecord;

    /** Panels that implement this are refreshed every time they are shown. */
    public interface Refreshable {
        void refresh();
    }

    // ── Constructor ─────────────────────────────────────────────────────

    public VotingAppFrame() {
        super("Online Voting System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setResizable(true);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(UIConstants.BG_DARK);
        setContentPane(cardPanel);

        // Register all screens
        addScreen(SCREEN_HOME,               new HomePanel(this));
        addScreen(SCREEN_ELECTION_SELECT,    new ElectionSelectionPanel(this));
        addScreen(SCREEN_CREATE_ELECTION,    new CreateElectionPanel(this));
        addScreen(SCREEN_ELECTION_MGMT,      new ElectionManagementPanel(this));
        addScreen(SCREEN_ADD_CANDIDATE,      new AddCandidatePanel(this));
        addScreen(SCREEN_ADD_VOTER,          new AddVoterPanel(this));
        addScreen(SCREEN_CANDIDATES,         new CandidatesPanel(this));
        addScreen(SCREEN_VOTERS,             new VotersPanel(this));
        addScreen(SCREEN_VOTING,             new VotingPanel(this));
        addScreen(SCREEN_RESULTS,            new ResultsPanel(this));
        addScreen(SCREEN_PREVIOUS_ELECTIONS, new PreviousElectionsPanel(this));
        addScreen(SCREEN_ELECTION_DETAILS,   new ElectionDetailsPanel(this));
        addScreen(SCREEN_RE_ELECTION,        new ReElectionPanel(this));
        addScreen(SCREEN_PREVIOUS_ELECTION_ACTION, new PreviousElectionActionPanel(this));

        showScreen(SCREEN_HOME);
    }

    // ── Screen management ───────────────────────────────────────────────

    public void addScreen(String name, JPanel panel) {
        screens.put(name, panel);
        cardPanel.add(panel, name);
    }

    /** Switches to the named screen; refreshes it first if it is {@link Refreshable}. */
    public void showScreen(String name) {
        JPanel panel = screens.get(name);
        if (panel instanceof Refreshable) {
            ((Refreshable) panel).refresh();
        }
        cardLayout.show(cardPanel, name);
    }

    /** Returns a registered screen panel, cast to the expected type. */
    @SuppressWarnings("unchecked")
    public <T extends JPanel> T getScreen(String name) {
        return (T) screens.get(name);
    }

    // ── Shared-state accessors ──────────────────────────────────────────

    public VotingManager getCurrentManager()                      { return currentManager; }
    public void setCurrentManager(VotingManager manager)          { this.currentManager = manager; }

    public ElectionDAO.ElectionRecord getCurrentRecord()          { return currentRecord; }
    public void setCurrentRecord(ElectionDAO.ElectionRecord rec)  { this.currentRecord = rec; }

    // ── Entry point ─────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) { }

            // Global dark-theme defaults
            UIManager.put("Panel.background",             UIConstants.BG_DARK);
            UIManager.put("OptionPane.background",        UIConstants.BG_DARK);
            UIManager.put("OptionPane.messageForeground", UIConstants.TEXT_PRIMARY);
            UIManager.put("Label.foreground",             UIConstants.TEXT_PRIMARY);
            UIManager.put("TextField.background",         UIConstants.BG_FIELD);
            UIManager.put("TextField.foreground",         UIConstants.TEXT_PRIMARY);
            UIManager.put("TextField.caretForeground",    UIConstants.TEXT_PRIMARY);

            VotingAppFrame frame = new VotingAppFrame();
            frame.setVisible(true);
        });
    }
}

class VotingPanel extends JPanel
        implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private final ElectionDAO    electionDAO = new ElectionDAO();

    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            electionNameLabel;
    private JLabel            statusValueLabel;
    private JLabel            timeRemainingLabel;
    private JLabel            votesCastLabel;
    private javax.swing.Timer             countdownTimer;

    public VotingPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        buildUI();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  UI Construction
    // ═══════════════════════════════════════════════════════════════════

    private void buildUI() {
        // ── Top section: header + stats ─────────────────────────────────
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        // Header row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        electionNameLabel = new JLabel("Election — Voting");
        electionNameLabel.setFont(UIConstants.FONT_HEADER);
        electionNameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        headerRow.add(electionNameLabel, BorderLayout.WEST);

        JLabel openBadge = new JLabel("  OPEN  ");
        openBadge.setFont(UIConstants.FONT_SMALL);
        openBadge.setForeground(UIConstants.ACCENT_GREEN);
        openBadge.setOpaque(true);
        openBadge.setBackground(new Color(0x12, 0x2E, 0x1A));
        openBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        headerRow.add(openBadge, BorderLayout.EAST);

        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(headerRow);
        top.add(Box.createVerticalStrut(20));

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        statusValueLabel   = new JLabel("Voting in progress");
        timeRemainingLabel = new JLabel("--:--");
        votesCastLabel     = new JLabel("0 / 0");

        statsRow.add(createStatCard("Status",         statusValueLabel));
        statsRow.add(createStatCard("Time Remaining", timeRemainingLabel));
        statsRow.add(createStatCard("Votes Cast",     votesCastLabel));

        top.add(statsRow);
        top.add(Box.createVerticalStrut(20));

        add(top, BorderLayout.NORTH);

        // ── Center: candidate table ────────────────────────────────────
        tableModel = new DefaultTableModel(
                new String[]{"ID", "Candidate", "Party", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3;            // only the Vote button column
            }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        TableColumn actionCol = table.getColumnModel().getColumn(3);
        actionCol.setPreferredWidth(100);
        actionCol.setMaxWidth(120);
        actionCol.setCellRenderer(new VoteButtonRenderer());
        actionCol.setCellEditor(new VoteButtonEditor());

        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);

        // ── Bottom buttons ─────────────────────────────────────────────
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        
        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftNav.setOpaque(false);

        JButton viewCandBtn = UIConstants.createSecondaryButton("View Candidates");
        viewCandBtn.setPreferredSize(new Dimension(160, 40));
        viewCandBtn.addActionListener(e -> {
            CandidatesPanel cp = frame.getScreen(VotingAppFrame.SCREEN_CANDIDATES);
            cp.setReturnScreen(VotingAppFrame.SCREEN_VOTING);
            frame.showScreen(VotingAppFrame.SCREEN_CANDIDATES);
        });
        leftNav.add(viewCandBtn);

        JButton viewVoterBtn = UIConstants.createSecondaryButton("View Voters");
        viewVoterBtn.setPreferredSize(new Dimension(140, 40));
        viewVoterBtn.addActionListener(e -> {
            VotersPanel vp = frame.getScreen(VotingAppFrame.SCREEN_VOTERS);
            vp.setReturnScreen(VotingAppFrame.SCREEN_VOTING);
            frame.showScreen(VotingAppFrame.SCREEN_VOTERS);
        });
        leftNav.add(viewVoterBtn);

        JButton liveResultsBtn = UIConstants.createSecondaryButton("Live Results");
        liveResultsBtn.setPreferredSize(new Dimension(140, 40));
        liveResultsBtn.addActionListener(e -> {
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_VOTING);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        });
        leftNav.add(liveResultsBtn);
        
        bottom.add(leftNav, BorderLayout.WEST);

        JPanel rightAction = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightAction.setOpaque(false);
        JButton endBtn = UIConstants.createDangerButton("End Voting");
        endBtn.setPreferredSize(new Dimension(140, 40));
        endBtn.addActionListener(e -> endVoting());
        rightAction.add(endBtn);
        
        bottom.add(rightAction, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
    }

    // ── Stat card helper ────────────────────────────────────────────────

    private JPanel createStatCard(String label, JLabel valueLabel) {
        JPanel card = new UIConstants.RoundedPanel(8, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

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

    // ═══════════════════════════════════════════════════════════════════
    //  Actions
    // ═══════════════════════════════════════════════════════════════════

    private void handleVote(String candidateId) {
        String voterId = JOptionPane.showInputDialog(frame,
                "Enter your Voter ID:",
                "Cast Vote", JOptionPane.PLAIN_MESSAGE);
        if (voterId == null || voterId.trim().isEmpty()) return;

        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            manager.castVote(voterId.trim(), candidateId);
            JOptionPane.showMessageDialog(frame,
                    "Vote submitted successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshStats();
        } catch (VotingException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Vote Rejected", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void endVoting() {
        Object[] options = {"Cancel", "OK"};
        int confirm = JOptionPane.showOptionDialog(frame,
                "Are you sure you want to end voting?",
                "End Voting",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1]);
        if (confirm != 1) return;

        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            electionDAO.updateStatus(manager.getElectionId(),
                    ElectionDAO.Status.COMPLETED);
            stopTimer();
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_HOME);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error ending voting: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Timer
    // ═══════════════════════════════════════════════════════════════════

    private void startTimer() {
        stopTimer();
        countdownTimer = new javax.swing.Timer(1000, e -> updateCountdown());
        countdownTimer.start();
    }

    private void stopTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void updateCountdown() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) { stopTimer(); return; }

        try {
            LocalDateTime end = manager.getElectionEnd();
            Duration remaining = Duration.between(LocalDateTime.now(), end);

            if (remaining.isNegative() || remaining.isZero()) {
                stopTimer();
                timeRemainingLabel.setText("00:00");
                statusValueLabel.setText("Voting ended");
                electionDAO.updateStatus(manager.getElectionId(),
                        ElectionDAO.Status.COMPLETED);

                JOptionPane.showMessageDialog(frame,
                        "Election voting window has ended.",
                        "Time's Up", JOptionPane.INFORMATION_MESSAGE);

                ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
                rp.setReturnScreen(VotingAppFrame.SCREEN_HOME);
                frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
                return;
            }

            long totalSecs = remaining.getSeconds();
            timeRemainingLabel.setText(
                    String.format("%02d:%02d", totalSecs / 60, totalSecs % 60));
            refreshStats();

        } catch (SQLException ex) {
            timeRemainingLabel.setText("Error");
        }
    }

    private void refreshStats() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            int votesCast  = manager.getVotes().size();
            int totalVoters = manager.getVoters().size();
            votesCastLabel.setText(votesCast + " / " + totalVoters);
        } catch (SQLException ignored) { }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Refreshable
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void refresh() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        try {
            electionNameLabel.setText(
                    manager.getElectionName() + " — Voting");

            tableModel.setRowCount(0);
            Collection<Candidate> candidates = manager.getCandidates();
            for (Candidate c : candidates) {
                tableModel.addRow(new Object[]{
                        c.getCandidateId(), c.getName(),
                        c.getPoliticalParty(), "Vote"
                });
            }

            statusValueLabel.setText("Voting in progress");
            refreshStats();
            startTimer();

        } catch (SQLException ex) {
            electionNameLabel.setText("Error loading election");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Vote Button (table cell renderer / editor)
    // ═══════════════════════════════════════════════════════════════════

    /** Renders a blue "Vote" label in every Action cell. */
    private class VoteButtonRenderer extends JLabel
            implements TableCellRenderer {

        VoteButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setBackground(UIConstants.PRIMARY_BLUE);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            setText("Vote");
            return this;
        }
    }

    /** Clickable editor that prompts for voter ID and casts the vote. */
    private class VoteButtonEditor extends AbstractCellEditor
            implements TableCellEditor {

        private final JButton button;

        VoteButtonEditor() {
            button = new JButton("Vote");
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(UIConstants.PRIMARY_BLUE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                int row = table.getEditingRow();
                fireEditingStopped();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    SwingUtilities.invokeLater(() -> {
                        String candidateId =
                                (String) tableModel.getValueAt(row, 0);
                        handleVote(candidateId);
                    });
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Vote";
        }
    }
}

