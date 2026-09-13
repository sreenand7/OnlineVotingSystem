package com.voting.gui;

import com.voting.dao.ElectionDAO;
import com.voting.manager.VotingManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Screen 11 — Previous Elections list.
 *
 * <p>Shows a table of all elections (UPCOMING, ACTIVE, COMPLETED). Allows selecting one to
 * view its results, view its details, export results, or conduct a re-election.</p>
 */
public class PreviousElectionsPanel extends JPanel
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
