package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window for the Online Voting System GUI.
 *
 * <p>Uses a single JFrame with CardLayout to host all screens.
 * Panels are registered by name and switched via {@link #showScreen(String)}.
 * Shared state (the current {@link VotingManager} and
 * {@link ElectionDAO.ElectionRecord}) is held here so every panel can
 * access it.</p>
 */
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
