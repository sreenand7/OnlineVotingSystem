package com.voting;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.*;

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
                    String.format("Candidate '%s' registered! (ID: %s)", name, c.getCandidateId()),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Cannot Add Candidate", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
    private JTextField dobField;

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
        card.add(Box.createVerticalStrut(20));

        card.add(UIConstants.createFieldLabel("Date of Birth"));
        card.add(Box.createVerticalStrut(8));
        dobField = UIConstants.createStyledField();
        dobField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(dobField);
        card.add(Box.createVerticalStrut(8));
        card.add(UIConstants.createMutedLabel(
                "Format: dd/MM/yyyy  —  the initial PIN is generated as first name + DOB."));
        card.add(Box.createVerticalStrut(24));

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
        String dobText = dobField.getText().trim();
        if (id.isEmpty() || name.isEmpty() || dobText.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Voter ID, name and date of birth are all required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
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

        LocalDate dob;
        try {
            dob = VotingManager.parseDateOfBirth(dobText);
        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(frame,
                    "Date of birth must be in dd/MM/yyyy format (e.g. 05/12/2000).",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String generatedPin =
                    frame.getCurrentManager().registerVoter(new Voter(id, name, dob));
            if (generatedPin == null) {
                JOptionPane.showMessageDialog(frame,
                        "Voter " + id + " already existed — enrolled in this election.\n"
                        + "Their existing PIN was left unchanged.",
                        "Voter Enrolled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame,
                        "Voter '" + name + "' registered.\n\n"
                        + "Initial PIN: " + generatedPin + "\n\n"
                        + "Share this PIN with the voter. It is not stored in plain text.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
            clearFields();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException | VotingException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        idField.setText("");
        nameField.setText("");
        dobField.setText("");
    }
}

class VoterLoginPanel extends JPanel {

    private final VotingAppFrame frame;
    private JTextField     idField;
    private JPasswordField pinField;

    public VoterLoginPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel header = new JLabel("Voter Login");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(8));
        card.add(UIConstants.createMutedLabel(
                "Sign in with your voter ID and PIN to cast your ballot."));
        card.add(Box.createVerticalStrut(32));

        card.add(UIConstants.createFieldLabel("Voter ID"));
        card.add(Box.createVerticalStrut(8));
        idField = UIConstants.createStyledField();
        idField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(idField);
        card.add(Box.createVerticalStrut(20));

        card.add(UIConstants.createFieldLabel("PIN"));
        card.add(Box.createVerticalStrut(8));
        pinField = UIConstants.createStyledPasswordField();
        pinField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pinField);
        card.add(Box.createVerticalStrut(8));
        card.add(UIConstants.createMutedLabel(
                "Your initial PIN is issued by the administrator when you are registered."));
        card.add(Box.createVerticalStrut(28));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.setPreferredSize(new Dimension(120, 44));
        backBtn.addActionListener(e -> {
            pinField.setText("");
            frame.showScreen(VotingAppFrame.SCREEN_HOME);
        });
        btnRow.add(backBtn);

        JButton loginBtn = UIConstants.createPrimaryButton("Login");
        loginBtn.setPreferredSize(new Dimension(160, 44));
        loginBtn.addActionListener(e -> login());
        btnRow.add(loginBtn);
        card.add(btnRow);

        pinField.addActionListener(e -> login());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    private void login() {
        String voterId = idField.getText().trim();
        String pin     = new String(pinField.getPassword());
        pinField.setText("");

        if (voterId.isEmpty() || pin.isEmpty()) {
            warn("Enter both your Voter ID and PIN.");
            return;
        }

        try {
            if (!VotingManager.hasPin(voterId)) {
                warn("No PIN has been issued for this voter ID.\n"
                        + "Please contact the election administrator.");
                return;
            }
            if (!VotingManager.login(voterId, pin)) {
                warn("Incorrect PIN. Please try again.");
                return;
            }
            onLoginSuccess();
        } catch (VotingException ex) {
            warn(ex.getMessage());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onLoginSuccess() {
        idField.setText("");
        pinField.setText("");
        frame.showScreen(VotingAppFrame.SCREEN_VOTER_ELECTIONS);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Login Failed", JOptionPane.WARNING_MESSAGE);
    }
}

class AdminLoginPanel extends JPanel {

    private final VotingAppFrame frame;
    private JTextField     idField;
    private JPasswordField passwordField;

    public AdminLoginPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new UIConstants.RoundedPanel(16, UIConstants.BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JLabel header = new JLabel("Admin Login");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(8));
        card.add(UIConstants.createMutedLabel(
                "Administrator access is required to manage elections."));
        card.add(Box.createVerticalStrut(32));

        card.add(UIConstants.createFieldLabel("Admin ID"));
        card.add(Box.createVerticalStrut(8));
        idField = UIConstants.createStyledField();
        idField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(idField);
        card.add(Box.createVerticalStrut(20));

        card.add(UIConstants.createFieldLabel("Password"));
        card.add(Box.createVerticalStrut(8));
        passwordField = UIConstants.createStyledPasswordField();
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(32));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.setPreferredSize(new Dimension(120, 44));
        backBtn.addActionListener(e -> {
            passwordField.setText("");
            frame.showScreen(VotingAppFrame.SCREEN_HOME);
        });
        btnRow.add(backBtn);

        JButton loginBtn = UIConstants.createPrimaryButton("Login");
        loginBtn.setPreferredSize(new Dimension(160, 44));
        loginBtn.addActionListener(e -> login());
        btnRow.add(loginBtn);
        card.add(btnRow);

        passwordField.addActionListener(e -> login());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    private void login() {
        String adminId  = idField.getText().trim();
        String password = new String(passwordField.getPassword());
        passwordField.setText("");

        if (adminId.isEmpty() || password.isEmpty()) {
            warn("Enter both your Admin ID and password.");
            return;
        }
        try {
            if (!VotingManager.adminLogin(adminId, password)) {
                warn("Invalid Admin ID or password.");
                return;
            }
            idField.setText("");
            frame.showScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Login Failed", JOptionPane.WARNING_MESSAGE);
    }
}

class AdminDashboardPanel extends JPanel implements VotingAppFrame.Refreshable {
    private final VotingAppFrame frame;
    private JLabel  welcomeLabel;
    private JLabel  electionLabel;
    private JButton completeBtn;


    public AdminDashboardPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    private void buildUI() {
        // No back button here: the Logout button is the only way out of the
        // admin session, so Back can never silently end it.
        JPanel header = UIConstants.createHeaderPanel("Admin Dashboard", null, null);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JPanel infoCard = new UIConstants.RoundedPanel(12, UIConstants.BG_CARD);
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        welcomeLabel = new JLabel("Administrator");
        welcomeLabel.setFont(UIConstants.FONT_HEADER);
        welcomeLabel.setForeground(UIConstants.TEXT_PRIMARY);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(welcomeLabel);
        infoCard.add(Box.createVerticalStrut(10));

        electionLabel = new JLabel("No election selected");
        electionLabel.setFont(UIConstants.FONT_BODY);
        electionLabel.setForeground(UIConstants.TEXT_SECONDARY);
        electionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoCard.add(electionLabel);

        center.add(infoCard);
        center.add(Box.createVerticalStrut(28));

        JPanel grid = new JPanel(new GridLayout(5, 2, 16, 16));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JButton createBtn = UIConstants.createPrimaryButton("Create Election");
        createBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_CREATE_ELECTION));
        grid.add(createBtn);

        JButton selectBtn = UIConstants.createSecondaryButton("Select Election");
        selectBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_ELECTION_SELECT));
        grid.add(selectBtn);

        JButton votersBtn = UIConstants.createSecondaryButton("Voter Management");
        votersBtn.addActionListener(e -> openReadOrManage(VotingAppFrame.SCREEN_VOTERS));
        grid.add(votersBtn);

        JButton candidatesBtn = UIConstants.createSecondaryButton("Candidate Management");
        candidatesBtn.addActionListener(e -> openReadOrManage(VotingAppFrame.SCREEN_CANDIDATES));
        grid.add(candidatesBtn);

        JButton resultsBtn = UIConstants.createSecondaryButton("Results / Export");
        resultsBtn.addActionListener(e -> openResults());
        grid.add(resultsBtn);

        JButton previousBtn = UIConstants.createSecondaryButton("Previous Elections");
        previousBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS));
        grid.add(previousBtn);

        JButton reElectBtn = UIConstants.createSecondaryButton("Re-election");
        reElectBtn.addActionListener(e -> openReElection());
        grid.add(reElectBtn);

        // Voter/candidate management for an ACTIVE election is read-only, so the
        // dashboard needs its own way to close that election.
        completeBtn = UIConstants.createDangerButton("Complete Election");
        completeBtn.addActionListener(e -> completeSelectedElection());
        grid.add(completeBtn);

        JButton logoutBtn = UIConstants.createDangerButton("Logout");
        logoutBtn.addActionListener(e -> {
            VotingManager.adminLogout();
            frame.showScreen(VotingAppFrame.SCREEN_HOME);
        });
        grid.add(logoutBtn);

        center.add(grid);
        add(center, BorderLayout.CENTER);
    }

    private void openElectionScoped(String screen, String message) {
        if (frame.getCurrentManager() == null) {
            // Stay on the dashboard — the admin session and this screen are unaffected
            JOptionPane.showMessageDialog(frame, message, "No Election Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        frame.showScreen(screen);
    }

    private String selectedStatus() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return null;
        try {
            return manager.getStatus();
        } catch (SQLException ex) {
            return null;
        }
    }

    // Only an UPCOMING election can be edited, so it alone opens the management
    // screen. ACTIVE and COMPLETED both open the read-only view panels instead.
    private void openReadOrManage(String viewScreen) {
        String status = selectedStatus();
        if (status == null) {
            JOptionPane.showMessageDialog(frame, "Start or select an election first.",
                    "No Election Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if ("UPCOMING".equals(status)) {
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
            return;
        }
        openViewScreen(viewScreen);
    }

    // Both view panels already derive isUpcoming from the selected election's
    // status, so their Remove action is disabled for ACTIVE and COMPLETED.
    private void openViewScreen(String screen) {
        if (screen.equals(VotingAppFrame.SCREEN_VOTERS)) {
            VotersPanel vp = frame.getScreen(VotingAppFrame.SCREEN_VOTERS);
            vp.setReturnScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
        } else {
            CandidatesPanel cp = frame.getScreen(VotingAppFrame.SCREEN_CANDIDATES);
            cp.setReturnScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
        }
        frame.showScreen(screen);
    }

    // Final results only exist once voting has closed.
    private void openResults() {
        String status = selectedStatus();
        if (status == null) {
            openElectionScoped(VotingAppFrame.SCREEN_RESULTS, "Start or select an election first.");
            return;
        }
        if (!"COMPLETED".equals(status)) {
            JOptionPane.showMessageDialog(frame,
                    "Results are only available once the election is COMPLETED.\n"
                    + "This election is currently " + status + ".",
                    "Results Not Available", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
        rp.setReturnScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
        frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
    }

    // Re-election replaces a finished election, so it needs a COMPLETED one.
    private void openReElection() {
        String status = selectedStatus();
        if (status == null) {
            JOptionPane.showMessageDialog(frame, "Select a completed election first.",
                    "No Election Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (!"COMPLETED".equals(status)) {
            JOptionPane.showMessageDialog(frame,
                    "A re-election can only be conducted on a COMPLETED election.\n"
                    + "This election is currently " + status + ".",
                    "Re-election Not Available", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        frame.showScreen(VotingAppFrame.SCREEN_RE_ELECTION);
    }

    // Administrative action: ACTIVE -> COMPLETED, then straight to the final results.
    private void completeSelectedElection() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        Object[] options = {"Cancel", "OK"};
        int confirm = JOptionPane.showOptionDialog(frame,
                "Are you sure you want to complete this election?\n"
                + "Voting will close and results become final.",
                "Complete Election", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (confirm != 1) return;
        try {
            manager.completeElection();
            frame.setCurrentRecord(new ElectionDAO().findById(manager.getElectionId()));
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        } catch (IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        if (!VotingManager.isAdminAuthenticated()) {
            frame.showScreen(VotingAppFrame.SCREEN_ADMIN_LOGIN);
            return;
        }
        welcomeLabel.setText("Welcome, " + VotingManager.getAuthenticatedAdminId());
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) {
            electionLabel.setText("No election selected");
            completeBtn.setEnabled(false);
            return;
        }
        try {
            electionLabel.setText("Selected election: " + manager.getElectionName()
                    + "  (ID " + manager.getElectionId() + ")");
            completeBtn.setEnabled("ACTIVE".equals(manager.getStatus()));
        } catch (SQLException ex) {
            electionLabel.setText("Error loading election");
            completeBtn.setEnabled(false);
        }
    }
}

class VoterElectionListPanel extends JPanel implements VotingAppFrame.Refreshable {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final VotingAppFrame frame;
    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            summaryLabel;

    public VoterElectionListPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel("My Elections", null, null);
        add(header, BorderLayout.NORTH);

        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(UIConstants.FONT_BODY);
        summaryLabel.setForeground(UIConstants.TEXT_SECONDARY);
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(summaryLabel);
        top.add(Box.createVerticalStrut(16));
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Election", "Closes", "Your Vote"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        // Back returns to the voter login screen; the session is deliberately kept
        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.setPreferredSize(new Dimension(140, 44));
        backBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_VOTER_LOGIN));
        bottom.add(backBtn);

        JButton changePinBtn = UIConstants.createSecondaryButton("Change PIN");
        changePinBtn.setPreferredSize(new Dimension(160, 44));
        changePinBtn.addActionListener(e -> showChangePinDialog());
        bottom.add(changePinBtn);

        JButton voteBtn = UIConstants.createPrimaryButton("Cast Vote");
        voteBtn.setPreferredSize(new Dimension(160, 44));
        voteBtn.addActionListener(e -> openSelectedElection());
        bottom.add(voteBtn);

        // Logout is the only button here that ends the session
        JButton logoutBtn = UIConstants.createSecondaryButton("Logout");
        logoutBtn.setPreferredSize(new Dimension(140, 44));
        logoutBtn.addActionListener(e -> {
            VotingManager.logout();
            frame.showScreen(VotingAppFrame.SCREEN_HOME);
        });
        bottom.add(logoutBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void showChangePinDialog() {
        JPasswordField currentField = UIConstants.createStyledPasswordField();
        JPasswordField newField     = UIConstants.createStyledPasswordField();
        JPasswordField confirmField = UIConstants.createStyledPasswordField();

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(UIConstants.createFieldLabel("Current PIN"));
        box.add(Box.createVerticalStrut(6));
        box.add(currentField);
        box.add(Box.createVerticalStrut(12));
        box.add(UIConstants.createFieldLabel("New PIN"));
        box.add(Box.createVerticalStrut(6));
        box.add(newField);
        box.add(Box.createVerticalStrut(12));
        box.add(UIConstants.createFieldLabel("Confirm New PIN"));
        box.add(Box.createVerticalStrut(6));
        box.add(confirmField);

        int choice = JOptionPane.showConfirmDialog(frame, box, "Change PIN",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            clearPasswords(currentField, newField, confirmField);
            return;
        }

        // Read first, clear afterwards — clearing before getPassword() wipes the entry
        char[] current = currentField.getPassword();
        char[] fresh   = newField.getPassword();
        char[] confirm = confirmField.getPassword();
        clearPasswords(currentField, newField, confirmField);

        try {
            VotingManager.changePin(new String(current), new String(fresh),
                    new String(confirm));
            JOptionPane.showMessageDialog(frame, "Your PIN has been changed.",
                    "PIN Changed", JOptionPane.INFORMATION_MESSAGE);
            return;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Change PIN", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (VotingException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Change PIN", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    private void clearPasswords(JPasswordField... fields) {
        for (JPasswordField field : fields) {
            field.setText("");
        }
    }

    private void openSelectedElection() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(frame, "Select an election first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int electionId = Integer.parseInt((String) tableModel.getValueAt(row, 0));
        boolean alreadyVoted = "Voted".equals(tableModel.getValueAt(row, 3));
        if (alreadyVoted) {
            JOptionPane.showMessageDialog(frame,
                    "You have already voted in this election.",
                    "Already Voted", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            frame.setCurrentManager(new VotingManager(electionId));
            frame.setCurrentRecord(new ElectionDAO().findById(electionId));
            frame.showScreen(VotingAppFrame.SCREEN_VOTING);
        } catch (IllegalArgumentException | SQLException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        VoterSession session = VotingManager.getSession();
        if (!session.isLoggedIn()) {
            frame.showScreen(VotingAppFrame.SCREEN_VOTER_LOGIN);
            return;
        }
        summaryLabel.setText("Signed in as " + session.getVoterName()
                + " (Voter ID " + session.getVoterId() + ")");
        try {
            List<ElectionDAO.VoterElection> elections =
                    VotingManager.getEligibleActiveElections();
            for (ElectionDAO.VoterElection ve : elections) {
                tableModel.addRow(new Object[]{
                        String.valueOf(ve.election.electionId),
                        ve.election.name,
                        ve.election.endTime != null ? ve.election.endTime.format(FMT) : "N/A",
                        ve.hasVoted ? "Voted" : "Not voted"
                });
            }
            if (elections.isEmpty())
                summaryLabel.setText(summaryLabel.getText()
                        + "  —  no ACTIVE elections available right now.");
        } catch (SQLException ex) {
            summaryLabel.setText("Could not load elections: " + ex.getMessage());
        }
    }
}

class CandidatesPanel extends JPanel implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private DefaultTableModel    tableModel;
    private JTable               table;
    private String               returnScreen = VotingAppFrame.SCREEN_ELECTION_MGMT;
    private boolean              isUpcoming   = true;

    public CandidatesPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    public void setReturnScreen(String screen) { this.returnScreen = screen; }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel("Candidates", "← Back",
                () -> frame.showScreen(returnScreen));
        add(header, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Candidate", "Party", "Action"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3 && isUpcoming; }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);

        TableColumn actionCol = table.getColumnModel().getColumn(3);
        actionCol.setPreferredWidth(100);
        actionCol.setMaxWidth(120);
        actionCol.setCellRenderer(new RemoveCandidateButtonRenderer());
        actionCol.setCellEditor(new RemoveCandidateButtonEditor());

        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);
    }

    private void handleRemoveCandidate(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) return;
        String candidateId = tableModel.getValueAt(row, 0).toString();
        String candidateName = tableModel.getValueAt(row, 1).toString();

        Object[] options = {"Cancel", "Remove"};
        int confirm = JOptionPane.showOptionDialog(frame,
                "Are you sure you want to remove " + candidateName
                        + " from this election?",
                "Remove Candidate", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        if (confirm != 1) return;

        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            manager.removeCandidateFromElection(Integer.parseInt(candidateId));
            refresh();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Cannot Remove", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        if (frame.getCurrentManager() == null) return;
        ElectionDAO.ElectionRecord record = frame.getCurrentRecord();
        isUpcoming = record != null && "UPCOMING".equals(record.status);
        try {
            for (Candidate c : frame.getCurrentManager().getCandidates()) {
                tableModel.addRow(new Object[]{
                        c.getCandidateId(), c.getName(), c.getPoliticalParty(), "Remove"
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error loading candidates: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class RemoveCandidateButtonRenderer extends JLabel implements TableCellRenderer {
        RemoveCandidateButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setBackground(UIConstants.DANGER_RED);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            setText("Remove");
            setBackground(isUpcoming ? UIConstants.DANGER_RED : UIConstants.BTN_DARK);
            setForeground(isUpcoming ? Color.WHITE : UIConstants.TEXT_MUTED);
            return this;
        }
    }

    private class RemoveCandidateButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button;

        RemoveCandidateButtonEditor() {
            button = new JButton("Remove");
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(UIConstants.DANGER_RED);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                int row = table.getEditingRow();
                fireEditingStopped();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    SwingUtilities.invokeLater(() -> handleRemoveCandidate(row));
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            return button;
        }

        @Override
        public Object getCellEditorValue() { return "Remove"; }
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

        JLabel header = new JLabel("Create New Election");
        header.setFont(UIConstants.FONT_HEADER);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(32));

        card.add(UIConstants.createFieldLabel("Election Name"));
        card.add(Box.createVerticalStrut(8));
        nameField = UIConstants.createStyledField();
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameField);
        card.add(Box.createVerticalStrut(20));

        card.add(UIConstants.createFieldLabel("Duration (minutes)"));
        card.add(Box.createVerticalStrut(8));
        durationField = UIConstants.createStyledField();
        durationField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(durationField);
        card.add(Box.createVerticalStrut(8));

        card.add(UIConstants.createMutedLabel(
                "Voting closes automatically once this duration elapses."));
        card.add(Box.createVerticalStrut(32));

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

    private void createElection() {
        String name = nameField.getText().trim();
        String durStr = durationField.getText().trim();
        if (name.isEmpty()) { warn("Election name cannot be empty."); return; }
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
            frame.setCurrentRecord(new ElectionDAO().findById(manager.getElectionId()));
            clearFields();
            JOptionPane.showMessageDialog(frame,
                    String.format("Election '%s' created successfully! (ID: %d)",
                            name, manager.getElectionId()),
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        nameField.setText("");
        durationField.setText("");
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }
}

class ElectionDetailsPanel extends JPanel implements VotingAppFrame.Refreshable {

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
        setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));
        buildUI();
    }

    public void setReturnScreen(String screen) { this.returnScreen = screen; }

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
        center.add(Box.createVerticalStrut(24));

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
        center.add(Box.createVerticalStrut(24));

        JPanel grid = new JPanel(new GridLayout(3, 2, 16, 16));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JButton viewResultsBtn = UIConstants.createPrimaryButton("View Results");
        viewResultsBtn.addActionListener(e -> {
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        });
        grid.add(viewResultsBtn);

        JButton viewCandBtn = UIConstants.createSecondaryButton("View Candidates");
        viewCandBtn.addActionListener(e -> {
            CandidatesPanel cp = frame.getScreen(VotingAppFrame.SCREEN_CANDIDATES);
            cp.setReturnScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
            frame.showScreen(VotingAppFrame.SCREEN_CANDIDATES);
        });
        grid.add(viewCandBtn);

        JButton viewVoterBtn = UIConstants.createSecondaryButton("View Voters");
        viewVoterBtn.addActionListener(e -> {
            VotersPanel vp = frame.getScreen(VotingAppFrame.SCREEN_VOTERS);
            vp.setReturnScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
            frame.showScreen(VotingAppFrame.SCREEN_VOTERS);
        });
        grid.add(viewVoterBtn);

        JButton exportResultsBtn = UIConstants.createSecondaryButton("Export Results");
        exportResultsBtn.addActionListener(e -> exportResults());
        grid.add(exportResultsBtn);

        JButton reElectBtn = UIConstants.createSecondaryButton("Conduct Re-election");
        reElectBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_RE_ELECTION));
        grid.add(reElectBtn);

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.addActionListener(e -> frame.showScreen(returnScreen));
        grid.add(backBtn);

        center.add(grid);
        add(center, BorderLayout.CENTER);
    }

    private void exportResults() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("election_results_" + manager.getElectionId() + ".txt"));
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                manager.exportResults(fc.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(frame, "Results exported successfully!",
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException | IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error exporting results: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
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
                ? " (Re-election of ID " + record.parentElectionId + ")" : "";

        detailsLabel.setText("<html>"
                + "<h2 style='margin-top:0;'>" + record.name + "</h2>"
                + "<b>Election ID:</b> " + record.electionId + parentText + "<br><br>"
                + "<b>Status:</b> " + record.status + "<br>"
                + "<b>Start Time:</b> " + (record.startTime != null ? record.startTime.format(FMT) : "N/A") + "<br>"
                + "<b>End Time:</b> " + (record.endTime != null ? record.endTime.format(FMT) : "N/A") + "<br>"
                + "</html>");

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

class ElectionManagementPanel extends JPanel implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private JLabel  nameLabel;
    private JLabel  statusLabel;
    private JButton startBtn;
    private JButton completeBtn;
    private JButton addCandBtn;
    private JButton addVoterBtn;
    private JButton importVoterBtn;

    public ElectionManagementPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel(
                "Election Management", "← Back",
                () -> frame.showScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD));
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

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

        JPanel grid = new JPanel(new GridLayout(4, 2, 16, 16));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        addCandBtn = UIConstants.createSecondaryButton("Add Candidate");
        addCandBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_ADD_CANDIDATE));
        grid.add(addCandBtn);

        addVoterBtn = UIConstants.createSecondaryButton("Add Voter");
        addVoterBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_ADD_VOTER));
        grid.add(addVoterBtn);

        importVoterBtn = UIConstants.createSecondaryButton("Import Voter List");
        importVoterBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_IMPORT_VOTERS));
        grid.add(importVoterBtn);

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

        completeBtn = UIConstants.createDangerButton("Complete Election");
        completeBtn.addActionListener(e -> completeElection());
        grid.add(completeBtn);

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD));
        grid.add(backBtn);

        center.add(grid);
        add(center, BorderLayout.CENTER);
    }

    private void startElection() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            if (manager.getCandidates().isEmpty()) {
                warn("Register at least one candidate before starting."); return;
            }
            if (manager.getVoters().isEmpty()) {
                warn("Register at least one voter before starting."); return;
            }

            Object[] options = {"Cancel", "OK"};
            int confirm = JOptionPane.showOptionDialog(frame,
                    "Are you sure you want to start the election?", "Start Election",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, options, options[1]);
            if (confirm != 1) return;

            if (manager.getDurationMinutes() <= 0) {
                String input = JOptionPane.showInputDialog(frame,
                        "Enter voting duration in minutes:", "Duration Required",
                        JOptionPane.QUESTION_MESSAGE);
                if (input == null) return;
                manager.startElection(Integer.parseInt(input.trim()));
            } else {
                manager.startElection();
            }

            frame.setCurrentRecord(new ElectionDAO().findById(manager.getElectionId()));
            JOptionPane.showMessageDialog(frame,
                    "Election is now ACTIVE.\nVoters can log in and cast their votes.",
                    "Election Started", JOptionPane.INFORMATION_MESSAGE);
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
        } catch (NumberFormatException ex) {
            warn("Please enter a valid number.");
        } catch (IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Cannot Start", JOptionPane.WARNING_MESSAGE);
    }

    // Administrative action. Voters never see this control.
    private void completeElection() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        Object[] options = {"Cancel", "OK"};
        int confirm = JOptionPane.showOptionDialog(frame,
                "Are you sure you want to complete this election?\n"
                + "Voting will close and results become final.",
                "Complete Election", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (confirm != 1) return;
        try {
            manager.completeElection();
            frame.setCurrentRecord(new ElectionDAO().findById(manager.getElectionId()));
            ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
            rp.setReturnScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
            frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
        } catch (IllegalStateException | SQLException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            nameLabel.setText(manager.getElectionName());
            ElectionDAO.ElectionRecord record = new ElectionDAO().findById(manager.getElectionId());
            if (record != null) {
                frame.setCurrentRecord(record);
                statusLabel.setText(record.status);
                // Voter/candidate lists are editable only while the election is UPCOMING
                boolean editable = "UPCOMING".equals(record.status);
                addVoterBtn.setEnabled(editable);
                addCandBtn.setEnabled(editable);
                importVoterBtn.setEnabled(editable);
                switch (record.status) {
                    case "UPCOMING" -> {
                        statusLabel.setForeground(UIConstants.STATUS_UPCOMING);
                        startBtn.setEnabled(true);
                        completeBtn.setEnabled(false);
                    }
                    case "ACTIVE" -> {
                        statusLabel.setForeground(UIConstants.STATUS_ACTIVE);
                        startBtn.setEnabled(false);
                        completeBtn.setEnabled(true);
                    }
                    default -> {
                        statusLabel.setForeground(UIConstants.STATUS_COMPLETED);
                        startBtn.setEnabled(false);
                        completeBtn.setEnabled(false);
                    }
                }
            }
        } catch (SQLException ex) {
            nameLabel.setText("Error loading election");
        }
    }
}

class ElectionSelectionPanel extends JPanel implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private DefaultTableModel tableModel;
    private JTable            table;

    public ElectionSelectionPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel(
                "Select Election", "← Back",
                () -> frame.showScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD));
        add(header, BorderLayout.NORTH);

        JLabel hint = new JLabel(
                "Choose an existing election to make it the current election.");
        hint.setFont(UIConstants.FONT_BODY);
        hint.setForeground(UIConstants.TEXT_SECONDARY);
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(hint);
        top.add(Box.createVerticalStrut(16));
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Election", "Status", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 3; }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(420);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);

        TableColumn actionCol = table.getColumnModel().getColumn(3);
        actionCol.setPreferredWidth(110);
        actionCol.setMaxWidth(130);
        actionCol.setCellRenderer(new SelectButtonRenderer());
        actionCol.setCellEditor(new SelectButtonEditor());
        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.setPreferredSize(new Dimension(140, 44));
        backBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD));
        bottom.add(backBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void selectElection(int electionId) {
        try {
            VotingManager manager = new VotingManager(electionId);
            ElectionDAO.ElectionRecord record = new ElectionDAO().findById(electionId);
            frame.setCurrentManager(manager);
            frame.setCurrentRecord(record);

            if (record != null && "COMPLETED".equals(record.status)) {
                // Read-only details screen: no Add/Remove/Start/Complete controls
                ElectionDetailsPanel dp =
                        frame.getScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
                dp.setReturnScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
                frame.showScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
            } else {
                frame.showScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD);
            }
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        try {
            for (ElectionDAO.ElectionRecord record : VotingManager.getAllElections()) {
                tableModel.addRow(new Object[]{
                        String.valueOf(record.electionId),
                        record.name,
                        record.status,
                        "Select"
                });
            }
        } catch (IllegalStateException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private class SelectButtonRenderer extends JLabel implements TableCellRenderer {
        SelectButtonRenderer() {
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
            setText("Select");
            return this;
        }
    }

    private class SelectButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button;

        SelectButtonEditor() {
            button = new JButton("Select");
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(UIConstants.PRIMARY_BLUE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                int row = table.getEditingRow();
                fireEditingStopped();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    SwingUtilities.invokeLater(() ->
                            selectElection(Integer.parseInt(
                                    (String) tableModel.getValueAt(row, 0))));
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            return button;
        }

        @Override
        public Object getCellEditorValue() { return "Select"; }
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

        JLabel iconLabel = new JLabel(createBallotIcon());
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(24));

        JLabel title = new JLabel("Online Voting System");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Election Management & Voting");
        subtitle.setFont(UIConstants.FONT_SUBTITLE);
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(40));

        JButton btnAdmin = UIConstants.createPrimaryButton("Admin Login");
        btnAdmin.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_ADMIN_LOGIN));
        card.add(UIConstants.wrapButton(btnAdmin, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnVoter = UIConstants.createPrimaryButton("Voter Login");
        btnVoter.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_VOTER_LOGIN));
        card.add(UIConstants.wrapButton(btnVoter, 48));
        card.add(Box.createVerticalStrut(12));

        JButton btnExit = UIConstants.createSecondaryButton("Exit");
        btnExit.addActionListener(e -> { frame.dispose(); System.exit(0); });
        card.add(UIConstants.wrapButton(btnExit, 48));
        card.add(Box.createVerticalStrut(32));

        JLabel footer = new JLabel("Secure  |  Fair  |  Transparent");
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(UIConstants.TEXT_MUTED);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(footer);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;  gbc.gridy = 0;
        gbc.weightx = 1; gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);
    }

    private Icon createBallotIcon() {
        final int size = 56;
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 0x20));
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(0x3B, 0x82, 0xF6, 0x40));
                g2.fillOval(x + 8, y + 8, size - 16, size - 16);
                g2.setColor(UIConstants.PRIMARY_BLUE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int bx = x + 16, by = y + 18, bw = 24, bh = 20;
                g2.drawRoundRect(bx, by, bw, bh, 3, 3);
                g2.drawLine(bx + 8, by, bx + 16, by);
                g2.drawLine(bx + 8, by - 2, bx + 16, by - 2);
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int px = bx + 10, py = by - 8;
                g2.drawRect(px, py, 8, 10);
                g2.setColor(UIConstants.PRIMARY_BLUE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(px + 2, py + 5, px + 3, py + 7);
                g2.drawLine(px + 3, py + 7, px + 6, py + 3);
                g2.dispose();
            }
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}

class PreviousElectionActionPanel extends JPanel implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private JLabel nameLabel;
    private JLabel idLabel;
    private JLabel statusLabel;

    public PreviousElectionActionPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel(
                "Selected Election", "← Back",
                () -> frame.showScreen(VotingAppFrame.SCREEN_PREVIOUS_ELECTIONS));
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

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
            if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                manager.exportResults(fc.getSelectedFile().getAbsolutePath());
                JOptionPane.showMessageDialog(frame, "Results exported successfully!",
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException | IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error exporting results: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        ElectionDAO.ElectionRecord record = frame.getCurrentRecord();
        if (record == null) return;
        nameLabel.setText(record.name);
        idLabel.setText("ID: " + record.electionId);
        statusLabel.setText(record.status);
        switch (record.status) {
            case "UPCOMING" -> statusLabel.setForeground(UIConstants.STATUS_UPCOMING);
            case "ACTIVE" -> statusLabel.setForeground(UIConstants.STATUS_ACTIVE);
            default -> statusLabel.setForeground(UIConstants.STATUS_COMPLETED);
        }
    }
}

class PreviousElectionsPanel extends JPanel implements VotingAppFrame.Refreshable {

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
        JPanel header = UIConstants.createHeaderPanel("Previous Elections", null, null);
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

        table.getSelectionModel().addListSelectionListener(e ->
                selectBtn.setEnabled(table.getSelectedRow() >= 0));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton backBtn = UIConstants.createSecondaryButton("Back");
        backBtn.setPreferredSize(new Dimension(100, 44));
        backBtn.addActionListener(e -> frame.showScreen(VotingAppFrame.SCREEN_ADMIN_DASHBOARD));
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
                frame.showScreen(VotingAppFrame.SCREEN_ELECTION_DETAILS);
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
            for (ElectionDAO.ElectionRecord rec : electionDAO.getAllElections()) {
                tableModel.addRow(new Object[]{
                        rec.electionId, rec.name,
                        rec.endTime != null ? rec.endTime.format(FMT) : "N/A",
                        rec.status
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading elections: " + ex.getMessage(),
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
        if (name.isEmpty()) { warn("Election name cannot be empty."); return; }
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
        if (record != null) nameField.setText(record.name + " (Re-election)");
        durationField.setText("");
    }
}

class ResultsPanel extends JPanel implements VotingAppFrame.Refreshable {

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

    public void setReturnScreen(String screen) { this.returnScreen = screen; }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel("Election Results", null, null);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);

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

    private void exportResults() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("election_results.txt"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            manager.exportResults(fc.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(frame, "Results exported successfully!",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        // Final results only — no counts while the election is still running
        String status;
        try {
            status = manager.getStatus();
        } catch (SQLException ex) {
            return;
        }
        if (!"COMPLETED".equals(status)) {
            totalVotesLabel.setText("—");
            registeredVotersLabel.setText("—");
            turnoutLabel.setText("—");
            winnerLabel.setText("Not available yet");
            return;
        }

        try {
            Map<String, Long> tally = manager.getTallyMap();
            Map<String, Candidate> candidates = new LinkedHashMap<>();
            for (Candidate c : manager.getCandidates())
                candidates.put(c.getCandidateId(), c);

            long totalVotes  = tally.values().stream().mapToLong(Long::longValue).sum();
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
                    : String.format("%.1f%%", 100.0 * totalVotes / totalVoters));

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
                    winnerLabel.setText("TIE: " + winners.stream()
                            .map(id -> candidates.containsKey(id)
                                    ? candidates.get(id).getName() : id)
                            .collect(Collectors.joining(", ")));
                }
            } else {
                winnerLabel.setText("-");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading results: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

final class UIConstants {

    private UIConstants() {}

    // Colors
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

    // Fonts
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

    public static JButton createPrimaryButton(String text) {
        return new StyledButton(text, PRIMARY_BLUE, PRIMARY_HOVER, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return new StyledButton(text, BTN_DARK, BTN_DARK_HOVER, TEXT_PRIMARY);
    }

    public static JButton createDangerButton(String text) {
        return new StyledButton(text, DANGER_RED, DANGER_HOVER, Color.WHITE);
    }

    public static JTextField createStyledField() {
        return styleField(new JTextField());
    }

    public static JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setEchoChar('*');
        return styleField(field);
    }

    private static <T extends JTextField> T styleField(T field) {
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

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

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

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean selected, boolean focused, int row, int col) {
                super.getTableCellRendererComponent(t, val, selected, focused, row, col);
                if (!selected)
                    setBackground(row % 2 == 0 ? BG_DARK : TABLE_ROW_ALT);
                setForeground(TEXT_PRIMARY);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
        return table;
    }

    public static JScrollPane wrapInScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(TABLE_GRID));
        sp.getViewport().setBackground(BG_DARK);
        return sp;
    }

    public static JPanel createHeaderPanel(String title, String backText, Runnable onBack) {
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
                @Override public void mouseEntered(MouseEvent e) { backBtn.setForeground(TEXT_PRIMARY); }
                @Override public void mouseExited(MouseEvent e)  { backBtn.setForeground(TEXT_SECONDARY); }
            });
            header.add(backBtn, BorderLayout.EAST);
        }
        return header;
    }

    public static JPanel wrapButton(JButton button, int height) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrapper.add(button, BorderLayout.CENTER);
        return wrapper;
    }

    public static class RoundedPanel extends JPanel {
        private final int   radius;
        private final Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius  = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.setColor(new Color(0xFF, 0xFF, 0xFF, 0x08));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class StyledButton extends JButton {
        private final Color normalBg;
        private final Color hoverBg;
        private boolean hovering = false;

        public StyledButton(String text, Color normalBg, Color hoverBg, Color textColor) {
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
                @Override public void mouseEntered(MouseEvent e) { hovering = true;  repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovering ? hoverBg : normalBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
            if (normalBg.equals(BTN_DARK)) {
                g2.setColor(BTN_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

class ImportVoterPanel extends JPanel {

    private final VotingAppFrame frame;
    private DefaultTableModel previewModel;
    private JLabel summaryLabel;
    private JButton importBtn;
    private final List<Voter> parsedVoters = new ArrayList<>();

    public ImportVoterPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel(
                "Import Voter List", "← Back",
                () -> frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT));
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BorderLayout(0, 16));

        JPanel topArea = new JPanel();
        topArea.setOpaque(false);
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));

        JLabel instructions = new JLabel(
                "Select a CSV file with columns: Roll Number, Name, DOB (dd/MM/yyyy)");
        instructions.setFont(UIConstants.FONT_BODY);
        instructions.setForeground(UIConstants.TEXT_SECONDARY);
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(instructions);
        topArea.add(Box.createVerticalStrut(12));

        JButton chooseBtn = UIConstants.createSecondaryButton("Choose CSV File…");
        chooseBtn.setPreferredSize(new Dimension(200, 40));
        chooseBtn.setMaximumSize(new Dimension(200, 40));
        chooseBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooseBtn.addActionListener(e -> chooseFile());
        topArea.add(chooseBtn);
        topArea.add(Box.createVerticalStrut(12));

        summaryLabel = new JLabel(" ");
        summaryLabel.setFont(UIConstants.FONT_BODY);
        summaryLabel.setForeground(UIConstants.TEXT_PRIMARY);
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topArea.add(summaryLabel);
        topArea.add(Box.createVerticalStrut(8));

        center.add(topArea, BorderLayout.NORTH);

        previewModel = new DefaultTableModel(new String[]{"Roll No.", "Name", "DOB"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable previewTable = UIConstants.createStyledTable(previewModel);
        previewTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        previewTable.getColumnModel().getColumn(1).setPreferredWidth(320);
        previewTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        center.add(UIConstants.wrapInScrollPane(previewTable), BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JButton cancelBtn = UIConstants.createSecondaryButton("Cancel");
        cancelBtn.setPreferredSize(new Dimension(120, 44));
        cancelBtn.addActionListener(e -> {
            clearState();
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
        });
        bottom.add(cancelBtn, BorderLayout.WEST);

        importBtn = UIConstants.createPrimaryButton("Import");
        importBtn.setPreferredSize(new Dimension(160, 44));
        importBtn.setEnabled(false);
        importBtn.addActionListener(e -> doImport());
        bottom.add(importBtn, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Voter CSV File");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "CSV Files (*.csv)", "csv"));
        if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
            parseCsv(fc.getSelectedFile());
    }

    private void parseCsv(File file) {
        parsedVoters.clear();
        previewModel.setRowCount(0);
        importBtn.setEnabled(false);
        summaryLabel.setText(" ");

        List<String> errors = new ArrayList<>();

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) { showError("The CSV file is empty."); return; }

            String[] headers = headerLine.split(",", -1);
            if (headers.length < 3) {
                showError("""
                          CSV must have three columns: Roll Number, Name, DOB.\n
                          Example header:\n
                          Roll Number,Name,DOB\n""");
                return;
            }
            String col0 = headers[0].trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            String col1 = headers[1].trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            String col2 = headers[2].trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            if (!(col0.contains("roll") || col0.contains("id") || col0.contains("number"))
                    || !col1.contains("name")
                    || !(col2.contains("dob") || col2.contains("birth"))) {
                showError("""
                          CSV header must contain 'Roll Number', 'Name' and 'DOB' columns.\n
                          Found: \"""" + headers[0].trim() + "\", \""
                        + headers[1].trim() + "\", \"" + headers[2].trim() + "\"");
                return;
            }

            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                String[] parts = trimmed.split(",", -1);
                if (parts.length < 3) {
                    errors.add("Line " + lineNum + ": not enough columns.");
                    continue;
                }

                String rollStr = parts[0].trim();
                String name    = parts[1].trim();
                String dobStr  = parts[2].trim();

                int rollNo;
                try {
                    rollNo = Integer.parseInt(rollStr);
                    if (rollNo <= 0) {
                        errors.add("Line " + lineNum + ": Roll Number must be positive (\""
                                + rollStr + "\").");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNum + ": Invalid Roll Number (\""
                            + rollStr + "\").");
                    continue;
                }

                if (name.isEmpty()) {
                    errors.add("Line " + lineNum + ": Name is empty.");
                    continue;
                }

                LocalDate dob;
                try {
                    dob = VotingManager.parseDateOfBirth(dobStr);
                } catch (java.time.format.DateTimeParseException e) {
                    errors.add("Line " + lineNum + ": Invalid DOB (\"" + dobStr
                            + "\") — use dd/MM/yyyy.");
                    continue;
                }

                parsedVoters.add(new Voter(String.valueOf(rollNo), name, dob));
                previewModel.addRow(new Object[]{ rollNo, name, dobStr });
            }
        } catch (java.io.IOException ex) {
            showError("Cannot read file: " + ex.getMessage());
            return;
        }

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Some rows were skipped:\n\n");
            int shown = Math.min(errors.size(), 10);
            for (int i = 0; i < shown; i++)
                sb.append("• ").append(errors.get(i)).append("\n");
            if (errors.size() > 10)
                sb.append("… and ").append(errors.size() - 10).append(" more.\n");
            if (!parsedVoters.isEmpty())
                sb.append("\n").append(parsedVoters.size()).append(" valid voter(s) are shown in the preview.");
            JOptionPane.showMessageDialog(frame, sb.toString(),
                    "CSV Warnings", JOptionPane.WARNING_MESSAGE);
        }

        if (parsedVoters.isEmpty()) {
            summaryLabel.setText("No valid voters found in the file.");
            importBtn.setEnabled(false);
        } else {
            summaryLabel.setText(parsedVoters.size() + " voter(s) found.");
            importBtn.setEnabled(true);
        }
    }

    private void doImport() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null || parsedVoters.isEmpty()) return;
        try {
            VotingManager.ImportResult result = manager.importVoters(parsedVoters);
            int enrolled = parsedVoters.size() - result.alreadyEnrolled;

            StringBuilder msg = new StringBuilder("Import complete!\n\n");
            msg.append("• ").append(enrolled).append(" voter(s) enrolled in this election.\n");
            if (result.newlyRegistered > 0)
                msg.append("• ").append(result.newlyRegistered).append(" new voter(s) registered.\n");
            if (result.alreadyExisted > 0)
                msg.append("• ").append(result.alreadyExisted)
                   .append(" voter(s) already existed (reused).\n");
            if (result.alreadyEnrolled > 0)
                msg.append("• ").append(result.alreadyEnrolled)
                   .append(" voter(s) were already in this election (skipped).\n");

            if (!result.generatedPins.isEmpty()) {
                msg.append("\nInitial PINs to distribute:\n");
                result.generatedPins.forEach((id, pin) ->
                        msg.append("   Voter ").append(id).append("  →  ").append(pin).append("\n"));
                msg.append("\nThese are shown once. Only hashes are stored.");
            }

            JOptionPane.showMessageDialog(frame, msg.toString(),
                    "Import Successful", JOptionPane.INFORMATION_MESSAGE);
            clearState();
            frame.showScreen(VotingAppFrame.SCREEN_ELECTION_MGMT);
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error during import: " + ex.getMessage(),
                    "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearState() {
        parsedVoters.clear();
        previewModel.setRowCount(0);
        importBtn.setEnabled(false);
        summaryLabel.setText(" ");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "CSV Error", JOptionPane.ERROR_MESSAGE);
    }
}

class VotersPanel extends JPanel implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private DefaultTableModel    tableModel;
    private JTable               table;
    private String               returnScreen = VotingAppFrame.SCREEN_ELECTION_MGMT;
    private boolean              isUpcoming   = true;

    public VotersPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));
        buildUI();
    }

    public void setReturnScreen(String screen) { this.returnScreen = screen; }

    private void buildUI() {
        JPanel header = UIConstants.createHeaderPanel("Voters", "← Back",
                () -> frame.showScreen(returnScreen));
        add(header, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Voter", "Status", "Action"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3 && isUpcoming; }
        };
        table = UIConstants.createStyledTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);

        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                String status = val != null ? val.toString() : "";
                setForeground(status.startsWith("Voted")
                        ? UIConstants.ACCENT_GREEN : UIConstants.TEXT_SECONDARY);
                if (!sel)
                    setBackground(row % 2 == 0 ? UIConstants.BG_DARK : UIConstants.TABLE_ROW_ALT);
                setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                return this;
            }
        });

        TableColumn actionCol = table.getColumnModel().getColumn(3);
        actionCol.setPreferredWidth(100);
        actionCol.setMaxWidth(120);
        actionCol.setCellRenderer(new RemoveVoterButtonRenderer());
        actionCol.setCellEditor(new RemoveVoterButtonEditor());

        add(UIConstants.wrapInScrollPane(table), BorderLayout.CENTER);
    }

    private void handleRemoveVoter(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) return;
        String voterId = tableModel.getValueAt(row, 0).toString();
        String voterName = tableModel.getValueAt(row, 1).toString();
        String status = tableModel.getValueAt(row, 2).toString();

        if (status.startsWith("Voted")) {
            JOptionPane.showMessageDialog(frame,
                    "This voter has already voted and cannot be removed.",
                    "Cannot Remove", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object[] options = {"Cancel", "Remove"};
        int confirm = JOptionPane.showOptionDialog(frame,
                "Are you sure you want to remove voter " + voterId
                        + " (" + voterName + ") from this election?",
                "Remove Voter", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        if (confirm != 1) return;

        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            manager.removeVoterFromElection(Integer.parseInt(voterId));
            refresh();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Cannot Remove", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        if (frame.getCurrentManager() == null) return;
        ElectionDAO.ElectionRecord record = frame.getCurrentRecord();
        isUpcoming = record != null && "UPCOMING".equals(record.status);
        try {
            List<Vote> votes = frame.getCurrentManager().getVotes();
            Set<String> votedIds = new HashSet<>();
            for (Vote v : votes) votedIds.add(v.getVoter().getVoterId());

            for (Voter v : frame.getCurrentManager().getVoters()) {
                tableModel.addRow(new Object[]{
                        v.getVoterId(), v.getName(),
                        votedIds.contains(v.getVoterId()) ? "Voted \u2713" : "Not Voted",
                        "Remove"
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading voters: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class RemoveVoterButtonRenderer extends JLabel implements TableCellRenderer {
        RemoveVoterButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setBackground(UIConstants.DANGER_RED);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean focus, int row, int col) {
            setText("Remove");
            setBackground(isUpcoming ? UIConstants.DANGER_RED : UIConstants.BTN_DARK);
            setForeground(isUpcoming ? Color.WHITE : UIConstants.TEXT_MUTED);
            return this;
        }
    }

    private class RemoveVoterButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button;

        RemoveVoterButtonEditor() {
            button = new JButton("Remove");
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(UIConstants.DANGER_RED);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                int row = table.getEditingRow();
                fireEditingStopped();
                if (row >= 0 && row < tableModel.getRowCount()) {
                    SwingUtilities.invokeLater(() -> handleRemoveVoter(row));
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            return button;
        }

        @Override
        public Object getCellEditorValue() { return "Remove"; }
    }
}

public class VotingAppFrame extends JFrame {

    public static final String SCREEN_HOME               = "HOME";
    public static final String SCREEN_ELECTION_SELECT    = "ELECTION_SELECT";
    public static final String SCREEN_CREATE_ELECTION    = "CREATE_ELECTION";
    public static final String SCREEN_ELECTION_MGMT      = "ELECTION_MGMT";
    public static final String SCREEN_ADD_CANDIDATE      = "ADD_CANDIDATE";
    public static final String SCREEN_ADD_VOTER          = "ADD_VOTER";
    public static final String SCREEN_IMPORT_VOTERS      = "IMPORT_VOTERS";
    public static final String SCREEN_CANDIDATES         = "CANDIDATES";
    public static final String SCREEN_VOTERS             = "VOTERS";
    public static final String SCREEN_VOTING             = "VOTING";
    public static final String SCREEN_VOTER_LOGIN        = "VOTER_LOGIN";
    public static final String SCREEN_VOTER_ELECTIONS   = "VOTER_ELECTIONS";
    public static final String SCREEN_ADMIN_LOGIN       = "ADMIN_LOGIN";
    public static final String SCREEN_ADMIN_DASHBOARD   = "ADMIN_DASHBOARD";
    public static final String SCREEN_RESULTS            = "RESULTS";
    public static final String SCREEN_PREVIOUS_ELECTIONS = "PREVIOUS_ELECTIONS";
    public static final String SCREEN_ELECTION_DETAILS   = "ELECTION_DETAILS";
    public static final String SCREEN_RE_ELECTION        = "RE_ELECTION";
    public static final String SCREEN_PREVIOUS_ELECTION_ACTION = "PREVIOUS_ELECTION_ACTION";

    // Administrative screens. Guarded in showScreen() as well as in VotingManager,
    // so an unauthenticated session cannot reach election management.
    private static final Set<String> ADMIN_ONLY_SCREENS = Set.of(
            SCREEN_ADMIN_DASHBOARD, SCREEN_ELECTION_SELECT, SCREEN_CREATE_ELECTION,
            SCREEN_ELECTION_MGMT, SCREEN_ADD_CANDIDATE, SCREEN_ADD_VOTER,
            SCREEN_IMPORT_VOTERS, SCREEN_PREVIOUS_ELECTIONS, SCREEN_ELECTION_DETAILS,
            SCREEN_RE_ELECTION, SCREEN_PREVIOUS_ELECTION_ACTION);

    private final CardLayout            cardLayout;
    private final JPanel                cardPanel;
    private final Map<String, JPanel>   screens = new HashMap<>();

    private VotingManager               currentManager;
    private ElectionDAO.ElectionRecord  currentRecord;

    public interface Refreshable { void refresh(); }

    @SuppressWarnings("OverridableMethodCallInConstructor")
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

        addScreen(SCREEN_HOME,               new HomePanel(this));
        addScreen(SCREEN_ELECTION_SELECT,    new ElectionSelectionPanel(this));
        addScreen(SCREEN_CREATE_ELECTION,    new CreateElectionPanel(this));
        addScreen(SCREEN_ELECTION_MGMT,      new ElectionManagementPanel(this));
        addScreen(SCREEN_ADD_CANDIDATE,      new AddCandidatePanel(this));
        addScreen(SCREEN_ADD_VOTER,          new AddVoterPanel(this));
        addScreen(SCREEN_IMPORT_VOTERS,      new ImportVoterPanel(this));
        addScreen(SCREEN_CANDIDATES,         new CandidatesPanel(this));
        addScreen(SCREEN_VOTERS,             new VotersPanel(this));
        addScreen(SCREEN_VOTING,             new VotingPanel(this));
        addScreen(SCREEN_VOTER_LOGIN,        new VoterLoginPanel(this));
        addScreen(SCREEN_VOTER_ELECTIONS,   new VoterElectionListPanel(this));
        addScreen(SCREEN_ADMIN_LOGIN,       new AdminLoginPanel(this));
        addScreen(SCREEN_ADMIN_DASHBOARD,   new AdminDashboardPanel(this));
        addScreen(SCREEN_RESULTS,            new ResultsPanel(this));
        addScreen(SCREEN_PREVIOUS_ELECTIONS, new PreviousElectionsPanel(this));
        addScreen(SCREEN_ELECTION_DETAILS,   new ElectionDetailsPanel(this));
        addScreen(SCREEN_RE_ELECTION,        new ReElectionPanel(this));
        addScreen(SCREEN_PREVIOUS_ELECTION_ACTION, new PreviousElectionActionPanel(this));

        showScreen(SCREEN_HOME);
    }

    public void addScreen(String name, JPanel panel) {
        screens.put(name, panel);
        cardPanel.add(panel, name);
    }

    public void showScreen(String name) {
        // Central access control — mirrors the guards inside VotingManager
        if (ADMIN_ONLY_SCREENS.contains(name) && !VotingManager.isAdminAuthenticated()) {
            VotingManager.adminLogout();
            JOptionPane.showMessageDialog(this,
                    "Administrator login is required for this action.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            name = SCREEN_HOME;
        }
        JPanel panel = screens.get(name);
        if (panel instanceof Refreshable refreshable)
            refreshable.refresh();
        cardLayout.show(cardPanel, name);
    }

    @SuppressWarnings("unchecked")
    public <T extends JPanel> T getScreen(String name) {
        return (T) screens.get(name);
    }

    public VotingManager getCurrentManager()                      { return currentManager; }
    public void setCurrentManager(VotingManager manager)          { this.currentManager = manager; }
    public ElectionDAO.ElectionRecord getCurrentRecord()          { return currentRecord; }
    public void setCurrentRecord(ElectionDAO.ElectionRecord rec)  { this.currentRecord = rec; }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ignored) { }

            UIManager.put("Panel.background",             UIConstants.BG_DARK);
            UIManager.put("OptionPane.background",        UIConstants.BG_DARK);
            UIManager.put("OptionPane.messageForeground", UIConstants.TEXT_PRIMARY);
            UIManager.put("Label.foreground",             UIConstants.TEXT_PRIMARY);
            UIManager.put("TextField.background",         UIConstants.BG_FIELD);
            UIManager.put("TextField.foreground",         UIConstants.TEXT_PRIMARY);
            UIManager.put("TextField.caretForeground",    UIConstants.TEXT_PRIMARY);
            UIManager.put("PasswordField.background",     UIConstants.BG_FIELD);
            UIManager.put("PasswordField.foreground",     UIConstants.TEXT_PRIMARY);
            UIManager.put("PasswordField.caretForeground", UIConstants.TEXT_PRIMARY);
            UIManager.put("PasswordField.selectionBackground", UIConstants.PRIMARY_BLUE);
            UIManager.put("PasswordField.selectionForeground", UIConstants.TEXT_PRIMARY);

            new VotingAppFrame().setVisible(true);
        });
    }
}

class VotingPanel extends JPanel implements VotingAppFrame.Refreshable {

    private final VotingAppFrame frame;
    private final ElectionDAO    electionDAO = new ElectionDAO();

    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            electionNameLabel;
    private JLabel            voterLabel;
    private JLabel            statusValueLabel;
    private JLabel            timeRemainingLabel;
    private JLabel            votesCastLabel;
    private javax.swing.Timer countdownTimer;

    public VotingPanel(VotingAppFrame frame) {
        this.frame = frame;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        buildUI();
    }

    private void buildUI() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        electionNameLabel = new JLabel("Election — Voting");
        electionNameLabel.setFont(UIConstants.FONT_HEADER);
        electionNameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        electionNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(electionNameLabel);
        titleBox.add(Box.createVerticalStrut(4));
        voterLabel = new JLabel(" ");
        voterLabel.setFont(UIConstants.FONT_SMALL);
        voterLabel.setForeground(UIConstants.ACCENT_GREEN);
        voterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(voterLabel);
        headerRow.add(titleBox, BorderLayout.WEST);

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

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        statusValueLabel   = new JLabel("Voting in progress");
        timeRemainingLabel = new JLabel("--:--");
        votesCastLabel     = new JLabel("—");

        statsRow.add(createStatCard("Status",         statusValueLabel));
        statsRow.add(createStatCard("Time Remaining", timeRemainingLabel));
        statsRow.add(createStatCard("Your Vote",        votesCastLabel));

        top.add(statsRow);
        top.add(Box.createVerticalStrut(20));
        add(top, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Candidate", "Party", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 3; }
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

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftNav.setOpaque(false);

        JButton backBtn = UIConstants.createSecondaryButton("← Back");
        backBtn.setPreferredSize(new Dimension(120, 40));
        backBtn.addActionListener(e -> {
            stopTimer();
            frame.showScreen(VotingAppFrame.SCREEN_VOTER_ELECTIONS);
        });
        leftNav.add(backBtn);

        JButton viewCandBtn = UIConstants.createSecondaryButton("View Candidates");
        viewCandBtn.setPreferredSize(new Dimension(160, 40));
        viewCandBtn.addActionListener(e -> {
            CandidatesPanel cp = frame.getScreen(VotingAppFrame.SCREEN_CANDIDATES);
            cp.setReturnScreen(VotingAppFrame.SCREEN_VOTING);
            frame.showScreen(VotingAppFrame.SCREEN_CANDIDATES);
        });
        leftNav.add(viewCandBtn);

        JButton logoutBtn = UIConstants.createSecondaryButton("Logout");
        logoutBtn.setPreferredSize(new Dimension(120, 40));
        logoutBtn.addActionListener(e -> {
            stopTimer();
            VotingManager.logout();
            frame.showScreen(VotingAppFrame.SCREEN_HOME);
        });
        leftNav.add(logoutBtn);

        bottom.add(leftNav, BorderLayout.WEST);

        add(bottom, BorderLayout.SOUTH);
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

    private void handleVote(String candidateId) {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;

        if (!VotingManager.getSession().isLoggedIn()) {
            JOptionPane.showMessageDialog(frame, "Please log in before casting your vote.",
                    "Login Required", JOptionPane.WARNING_MESSAGE);
            frame.showScreen(VotingAppFrame.SCREEN_VOTER_LOGIN);
            return;
        }

        try {
            manager.castVote(candidateId);
            JOptionPane.showMessageDialog(frame, "Vote submitted successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            // Return to the voter's election list so they are never stranded here
            stopTimer();
            frame.showScreen(VotingAppFrame.SCREEN_VOTER_ELECTIONS);
        } catch (VotingException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(),
                    "Vote Rejected", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Database error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

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
            Duration remaining = Duration.between(LocalDateTime.now(), manager.getElectionEnd());
            if (remaining.isNegative() || remaining.isZero()) {
                stopTimer();
                timeRemainingLabel.setText("00:00");
                statusValueLabel.setText("Voting ended");
                electionDAO.updateStatus(manager.getElectionId(), ElectionDAO.Status.COMPLETED);
                JOptionPane.showMessageDialog(frame, "Election voting window has ended.",
                        "Time's Up", JOptionPane.INFORMATION_MESSAGE);
                ResultsPanel rp = frame.getScreen(VotingAppFrame.SCREEN_RESULTS);
                rp.setReturnScreen(VotingAppFrame.SCREEN_VOTER_ELECTIONS);
                frame.showScreen(VotingAppFrame.SCREEN_RESULTS);
                return;
            }
            long totalSecs = remaining.getSeconds();
            timeRemainingLabel.setText(String.format("%02d:%02d", totalSecs / 60, totalSecs % 60));
            refreshStats();
        } catch (SQLException ex) {
            timeRemainingLabel.setText("Error");
        }
    }

    private void refreshStats() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        if (!VotingManager.getSession().isLoggedIn()) {
            votesCastLabel.setText("—");
            return;
        }
        try {
            votesCastLabel.setText(manager.hasCurrentVoterVoted() ? "Voted" : "Not voted");
        } catch (SQLException ignored) { }
    }

    @Override
    public void refresh() {
        VotingManager manager = frame.getCurrentManager();
        if (manager == null) return;
        try {
            electionNameLabel.setText(manager.getElectionName() + " — Voting");
            VoterSession session = VotingManager.getSession();
            voterLabel.setText(session.isLoggedIn()
                    ? "Signed in as " + session.getVoterName() + " (ID " + session.getVoterId() + ")"
                    : "Not signed in — login required to vote");
            if (!session.isLoggedIn())
                voterLabel.setForeground(UIConstants.STATUS_UPCOMING);
            tableModel.setRowCount(0);
            for (Candidate c : manager.getCandidates()) {
                tableModel.addRow(new Object[]{
                        c.getCandidateId(), c.getName(), c.getPoliticalParty(), "Vote"
                });
            }
            statusValueLabel.setText("Voting in progress");
            refreshStats();
            startTimer();
        } catch (SQLException ex) {
            electionNameLabel.setText("Error loading election");
        }
    }

    private class VoteButtonRenderer extends JLabel implements TableCellRenderer {
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

    private class VoteButtonEditor extends AbstractCellEditor implements TableCellEditor {
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
                    SwingUtilities.invokeLater(() ->
                            handleVote((String) tableModel.getValueAt(row, 0)));
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object val,
                boolean sel, int row, int col) {
            return button;
        }

        @Override
        public Object getCellEditorValue() { return "Vote"; }
    }
}
