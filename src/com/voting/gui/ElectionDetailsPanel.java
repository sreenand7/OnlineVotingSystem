package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Screen 12 — Election Details.
 *
 * <p>Shows static details (name, times, status) and aggregate stats
 * (candidates count, voters count, votes cast) for a selected election.</p>
 */
public class ElectionDetailsPanel extends JPanel
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
