package com.voting;

import com.voting.dao.ElectionDAO;
import com.voting.dao.ElectionDAO.ElectionRecord;
import com.voting.manager.VotingManager;
import com.voting.model.Candidate;
import com.voting.model.Voter;
import com.voting.util.VotingException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the Online Voting System.
 *
 * Provides a menu-driven command-line interface:
 *   Main Menu — Start/Select Election, View Previous Elections, Exit
 *   New election → register candidates/voters → start voting → results
 *   Existing UPCOMING election → continue registration → start voting
 *   Existing ACTIVE election → voting menu
 *   COMPLETED election → view results, export, or conduct re-election
 *
 * Run using:
 * java -cp "output;lib/*" com.voting.Main
 */

public class Main {

    private static VotingManager manager;
    private static final Scanner sc = new Scanner(System.in);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ElectionDAO electionDAO = new ElectionDAO();

    public static void main(String[] args) {
        printBanner();
        mainMenuLoop();
        System.out.println("  Goodbye!\n");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Main Menu
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Top-level menu loop. Repeats until the user chooses Exit.
     */
    private static void mainMenuLoop() {
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> startOrSelectElection();
                case "2" -> viewPreviousElections();
                case "3" -> running = false;
                default  -> System.out.println("  Invalid option. Please choose 1-3.\n");
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│        ONLINE VOTING SYSTEM             │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1. Start / Select Election             │");
        System.out.println("│  2. View Previous Elections             │");
        System.out.println("│  3. Exit                                │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Option 1 — Start / Select Election
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lets the user create a brand-new election or attach to an existing one
     * by ID. Behavior after attachment depends on the election's status.
     */
    private static void startOrSelectElection() {
        System.out.println("  ── Start / Select Election ──\n");
        System.out.println("  1. Create a new election");
        System.out.println("  2. Enter an existing election ID");
        System.out.println("  0. Back to main menu");
        System.out.print("\n  Choose an option: ");
        String choice = sc.nextLine().trim();
        System.out.println();

        switch (choice) {
            case "1" -> createNewElection();
            case "2" -> attachToExistingElection();
            case "0" -> { /* back */ }
            default  -> System.out.println("  Invalid option.\n");
        }
    }

    /**
     * Creates a brand-new election: prompts for name + duration, then
     * enters the registration → voting → results workflow.
     */
    private static void createNewElection() {
        System.out.println("  ── Create New Election ──\n");

        System.out.print("  Enter election name: ");
        String name = sc.nextLine().trim();
        while (name.isEmpty()) {
            System.out.print("  Election name cannot be empty. Enter election name: ");
            name = sc.nextLine().trim();
        }

        // Handle duplicate names
        name = resolveElectionName(name);
        if (name == null) return;  // user cancelled

        int minutes = readPositiveInt("  Voting window duration in minutes: ");

        try {
            manager = new VotingManager(name, minutes);
            System.out.printf("%n  ✓  Election '%s' created (ID: %d, %d-minute window).%n",
                    name, manager.getElectionId(), minutes);
            System.out.println("  ✓  Timer will start when you choose \"Start Election\".\n");
        } catch (SQLException e) {
            System.out.println("  ✗ Failed to create election: " + e.getMessage() + "\n");
            return;
        }

        if (!registrationPhase()) return;  // user chose Back
        votingPhase();            // Phase 3: start election, cast votes
        resultsPhase();           // Phase 4: display / export
    }

    /**
     * Attaches to an existing election by ID. Routes to the correct
     * phase based on status: UPCOMING → registration, ACTIVE → voting,
     * COMPLETED → completed-election menu.
     */
    private static void attachToExistingElection() {
        int electionId = readPositiveInt("  Enter election ID: ");

        try {
            manager = new VotingManager(electionId);
        } catch (IllegalArgumentException e) {
            System.out.println("  ✗ " + e.getMessage() + "\n");
            return;
        } catch (SQLException e) {
            System.out.println("  ✗ Database error: " + e.getMessage() + "\n");
            return;
        }

        // Determine current status and branch accordingly
        try {
            ElectionRecord record = electionDAO.findById(electionId);
            if (record == null) {
                System.out.println("  ✗ Election not found.\n");
                return;
            }

            System.out.printf("  ✓  Attached to election '%s' (ID: %d, Status: %s)%n%n",
                    record.name, record.electionId, record.status);

            switch (record.status) {
                case "UPCOMING" -> {
                    System.out.println("  Election is UPCOMING — entering registration phase.\n");
                    if (!registrationPhase()) break;  // user chose Back
                    votingPhase();
                    resultsPhase();
                }
                case "ACTIVE" -> {
                    System.out.println("  Election is ACTIVE — entering voting phase.\n");
                    votingPhase();
                    resultsPhase();
                }
                case "COMPLETED" -> {
                    System.out.println("  Election is COMPLETED.\n");
                    completedElectionMenu(record);
                }
                default -> System.out.println("  ✗ Unknown election status: " + record.status + "\n");
            }
        } catch (SQLException e) {
            System.out.println("  ✗ Database error: " + e.getMessage() + "\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Option 2 — View Previous Elections
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Lists all completed elections and lets the user select one to
     * view results, candidates, details, or conduct a re-election.
     */
    private static void viewPreviousElections() {
        System.out.println("  ── Previous Elections ──\n");

        List<ElectionRecord> completed;
        try {
            completed = electionDAO.getCompletedElections();
        } catch (SQLException e) {
            System.out.println("  ✗ Failed to load elections: " + e.getMessage() + "\n");
            return;
        }

        if (completed.isEmpty()) {
            System.out.println("  No completed elections found.\n");
            return;
        }

        // Display table of completed elections
        System.out.println("  " + "-".repeat(78));
        System.out.printf("  %-6s  %-28s  %-20s  %-10s  %s%n",
                "ID", "Name", "End Time", "Status", "Parent ID");
        System.out.println("  " + "-".repeat(78));
        for (ElectionRecord rec : completed) {
            System.out.printf("  %-6d  %-28s  %-20s  %-10s  %s%n",
                    rec.electionId,
                    truncate(rec.name, 28),
                    rec.endTime.format(DATETIME_FMT),
                    rec.status,
                    rec.parentElectionId == null ? "-" : String.valueOf(rec.parentElectionId));
        }
        System.out.println("  " + "-".repeat(78));
        System.out.println();

        // Let user select one
        int selectedId = readNonNegativeInt("  Enter election ID to view (or 0 to go back): ");
        if (selectedId == 0) return;

        // Verify the selected ID is among the completed elections
        ElectionRecord selected = null;
        for (ElectionRecord rec : completed) {
            if (rec.electionId == selectedId) {
                selected = rec;
                break;
            }
        }
        if (selected == null) {
            System.out.println("  ✗ Election ID " + selectedId + " is not in the completed list.\n");
            return;
        }

        // Attach manager to the selected completed election
        try {
            manager = new VotingManager(selectedId);
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("  ✗ Failed to load election: " + e.getMessage() + "\n");
            return;
        }

        completedElectionMenu(selected);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Completed Election Menu
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Menu for a completed election: view results, candidates, export,
     * or conduct a re-election.
     */
    private static void completedElectionMenu(ElectionRecord record) {
        boolean viewing = true;
        while (viewing) {
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.printf ("│  COMPLETED: %-27s  │%n", truncate(record.name, 27));
            System.out.println("├─────────────────────────────────────────┤");
            System.out.println("│  1. View election results               │");
            System.out.println("│  2. View candidates                     │");
            System.out.println("│  3. View election details               │");
            System.out.println("│  4. Export results to file              │");
            System.out.println("│  5. Conduct re-election                 │");
            System.out.println("│  0. Back                                │");
            System.out.println("└─────────────────────────────────────────┘");
            System.out.print("  Choose an option: ");
            String choice = sc.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1" -> {
                    try {
                        manager.displayResults();
                    } catch (SQLException e) {
                        System.out.println("  ✗ Error displaying results: " + e.getMessage() + "\n");
                    }
                }
                case "2" -> listCandidates();
                case "3" -> displayElectionDetails(record);
                case "4" -> exportReport();
                case "5" -> {
                    conductReElection(record);
                    viewing = false; // After re-election flow, return to main menu
                }
                case "0" -> viewing = false;
                default  -> System.out.println("  Invalid option. Please choose 0-5.\n");
            }
        }
    }

    /**
     * Displays detailed information about a completed election.
     */
    private static void displayElectionDetails(ElectionRecord record) {
        System.out.println("  ── Election Details ──\n");
        System.out.println("  Election ID    : " + record.electionId);
        System.out.println("  Name           : " + record.name);
        System.out.println("  Status         : " + record.status);
        System.out.println("  Start Time     : " + record.startTime.format(DATETIME_FMT));
        System.out.println("  End Time       : " + record.endTime.format(DATETIME_FMT));
        System.out.println("  Parent ID      : " +
                (record.parentElectionId == null ? "None (original)" : record.parentElectionId));

        try {
            System.out.println("  Voters         : " + manager.getVoters().size());
            System.out.println("  Candidates     : " + manager.getCandidates().size());
            System.out.println("  Votes Cast     : " + manager.getVotes().size());
        } catch (SQLException e) {
            System.out.println("  ✗ Error loading stats: " + e.getMessage());
        }
        System.out.println();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Re-Election
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new election linked to a completed one via parent_election_id.
     * The old election is never modified.
     */
    private static void conductReElection(ElectionRecord parentRecord) {
        System.out.println("  ── Conduct Re-Election ──\n");
        System.out.printf("  Original election: '%s' (ID: %d)%n%n", parentRecord.name, parentRecord.electionId);

        System.out.print("  Enter new election name [Re: " + parentRecord.name + "]: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            name = "Re: " + parentRecord.name;
        }

        // Handle duplicate names
        name = resolveElectionName(name);
        if (name == null) return;  // user cancelled

        int minutes = readPositiveInt("  Voting window duration in minutes: ");

        try {
            manager = new VotingManager(name, minutes, parentRecord.electionId);
            System.out.printf("%n  ✓  Re-election '%s' created (ID: %d, Parent: %d, %d-minute window).%n",
                    name, manager.getElectionId(), parentRecord.electionId, minutes);
            System.out.println("  ✓  Timer will start when you choose \"Start Election\".\n");
        } catch (SQLException e) {
            System.out.println("  ✗ Failed to create re-election: " + e.getMessage() + "\n");
            return;
        }

        // Enter the full election workflow for the new election
        if (!registrationPhase()) return;  // user chose Back
        votingPhase();
        resultsPhase();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Phase 2 — Registration
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Loops over a registration-only menu until the operator explicitly
     * starts the election (option 5).
     *
     * @return {@code true} if the election was started, {@code false}
     *         if the user chose Back (callers should skip subsequent phases)
     */
    private static boolean registrationPhase() {
        System.out.println("  ── Phase 2: Registration ──\n");

        while (true) {
            printRegistrationMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> addCandidate();
                case "2" -> addVoter();
                case "3" -> listCandidates();
                case "4" -> listVoters();
                case "5" -> {
                    if (confirmStartElection()) {
                        return true;  // election started successfully
                    }
                }
                case "0" -> {
                    System.out.println("  Returning to main menu.\n");
                    return false; // back to main menu, do NOT proceed to voting
                }
                default  -> System.out.println("  Invalid option. Please choose 0-5.\n");
            }
        }
    }

    private static void printRegistrationMenu() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│          REGISTRATION  PHASE            │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1. Add candidate                       │");
        System.out.println("│  2. Add voter                           │");
        System.out.println("│  3. List candidates                     │");
        System.out.println("│  4. List registered voters              │");
        System.out.println("│  5. ▶  Start election                   │");
        System.out.println("│  0. Back to main menu                   │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }

    /**
     * Validates that at least one candidate and one voter exist,
     * then confirms and starts the election timer.
     */
    private static boolean confirmStartElection() {
        try {
            if (manager.getCandidates().isEmpty()) {
                System.out.println("  ✗ Register at least one candidate before starting.\n");
                return false;
            }
            if (manager.getVoters().isEmpty()) {
                System.out.println("  ✗ Register at least one voter before starting.\n");
                return false;
            }

            System.out.printf("  Ready to start '%s' with %d candidate(s) and %d voter(s).%n",
                    manager.getElectionName(),
                    manager.getCandidates().size(),
                    manager.getVoters().size());
            System.out.print("  Confirm start? (y/n): ");
            String confirm = sc.nextLine().trim().toLowerCase();

            if (confirm.equals("y") || confirm.equals("yes")) {
                // Use startElection(int) for attached UPCOMING elections (duration may be 0),
                // otherwise use startElection() which uses the pre-set duration.
                if (manager.getDurationMinutes() <= 0) {
                    int minutes = readPositiveInt("  Enter voting duration in minutes: ");
                    manager.startElection(minutes);
                } else {
                    manager.startElection();
                }
                System.out.printf("%n  ✓  Election started!%n");
                System.out.printf("  ✓  Voting window: %s  →  %s%n%n",
                        manager.getElectionStart().format(TIME_FMT),
                        manager.getElectionEnd().format(TIME_FMT));
                return true;
            }
            System.out.println("  Start cancelled — continue registering.\n");
            return false;
        } catch (SQLException e) {
            System.out.println("  ✗ Database error: " + e.getMessage() + "\n");
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Phase 3 — Voting
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Loops over the voting menu until the operator ends the election
     * or the time window expires.
     */
    private static void votingPhase() {
        System.out.println("  ── Phase 3: Voting ──\n");

        boolean voting = true;
        while (voting) {
            // Auto-detect window expiry
            try {
                if (!manager.isElectionOpen()) {
                    System.out.println("  ⏰  Election voting window has ended.\n");
                    break;
                }
            } catch (SQLException e) {
                System.out.println("  ✗ Error checking election status: " + e.getMessage() + "\n");
                break;
            }

            printVotingMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> castVoteInteractive();
                case "2" -> listCandidates();
                case "3" -> listVoters();
                case "4" -> {
                    try {
                        manager.displayResults();
                    } catch (SQLException e) {
                        System.out.println("  ✗ Error displaying results: " + e.getMessage() + "\n");
                    }
                }
                case "5" -> {
                    System.out.println("  Ending voting phase.\n");
                    voting = false;
                }
                default  -> System.out.println("  Invalid option. Please choose 1-5.\n");
            }
        }

        // Mark the election as COMPLETED in the database
        try {
            electionDAO.updateStatus(manager.getElectionId(), ElectionDAO.Status.COMPLETED);
            System.out.println("  ✓  Election marked as COMPLETED.\n");
        } catch (SQLException e) {
            System.out.println("  ✗ Failed to mark election as completed: " + e.getMessage() + "\n");
        }
    }

    private static void printVotingMenu() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│            VOTING  PHASE                │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1. Cast a vote                         │");
        System.out.println("│  2. List candidates                     │");
        System.out.println("│  3. List registered voters              │");
        System.out.println("│  4. Display live results                │");
        System.out.println("│  5. End voting & view results           │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Phase 4 — Results
    // ═══════════════════════════════════════════════════════════════════════════

    private static void resultsPhase() {
        System.out.println("  ── Phase 4: Results ──\n");
        try {
            manager.displayResults();
        } catch (SQLException e) {
            System.out.println("  ✗ Error displaying results: " + e.getMessage() + "\n");
        }

        boolean viewing = true;
        while (viewing) {
            printResultsMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> {
                    try {
                        manager.displayResults();
                    } catch (SQLException e) {
                        System.out.println("  ✗ Error displaying results: " + e.getMessage() + "\n");
                    }
                }
                case "2" -> exportReport();
                case "0" -> viewing = false;
                default  -> System.out.println("  Invalid option. Please choose 0-2.\n");
            }
        }
    }

    private static void printResultsMenu() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│           RESULTS  PHASE                │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1. Display election results            │");
        System.out.println("│  2. Export results to file              │");
        System.out.println("│  0. Back to main menu                   │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Shared option handlers
    // ═══════════════════════════════════════════════════════════════════════════

    private static void addCandidate() {
        System.out.print("  Enter Candidate Name  : ");
        String name = sc.nextLine().trim();
        System.out.print("  Enter Political Party : ");
        String party = sc.nextLine().trim();

        try {
            Candidate c = manager.registerCandidate(name, party);
            System.out.printf("  ✓ Candidate registered (ID: %s).%n%n", c.getCandidateId());
        } catch (IllegalArgumentException e) {
            System.out.println("  ✗ Could not register candidate: " + e.getMessage() + "\n");
        } catch (SQLException e) {
            System.out.println("  ✗ Database error: " + e.getMessage() + "\n");
        }
    }

    private static void addVoter() {
        // Validate numeric Voter ID before asking for name
        String id;
        while (true) {
            System.out.print("  Enter Voter ID (0 to cancel): ");
            id = sc.nextLine().trim();
            if (id.equals("0")) {
                System.out.println("  Voter registration cancelled.\n");
                return;
            }
            try {
                int parsed = Integer.parseInt(id);
                if (parsed > 0) break;
                System.out.println("  Voter ID must be a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid Voter ID — must be a numeric value.");
            }
        }

        System.out.print("  Enter Voter Name : ");
        String name = sc.nextLine().trim();

        try {
            manager.registerVoter(new Voter(id, name));
            System.out.println("  ✓ Voter registered.\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✗ Could not register voter: " + e.getMessage() + "\n");
        } catch (SQLException | VotingException e) {
            System.out.println("  ✗ Error: " + e.getMessage() + "\n");
        }
    }

    private static void listCandidates() {
        try {
            var candidates = manager.getCandidates();
            if (candidates.isEmpty()) {
                System.out.println("  No candidates registered yet.\n");
                return;
            }
            System.out.println("  Registered Candidates:");
            System.out.println("  " + "-".repeat(54));
            System.out.printf ("  %-8s  %-22s  %s%n", "ID", "Name", "Party");
            System.out.println("  " + "-".repeat(54));
            for (Candidate c : candidates) {
                System.out.printf("  %-8s  %-22s  %s%n",
                        c.getCandidateId(), c.getName(), c.getPoliticalParty());
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("  ✗ Error loading candidates: " + e.getMessage() + "\n");
        }
    }

    private static void listVoters() {
        try {
            var voters = manager.getVoters();
            if (voters.isEmpty()) {
                System.out.println("  No voters registered yet.\n");
                return;
            }
            System.out.println("  Registered Voters:");
            System.out.println("  " + "-".repeat(46));
            System.out.printf ("  %-8s  %-22s  %s%n", "ID", "Name", "Voted?");
            System.out.println("  " + "-".repeat(46));
            for (Voter v : voters) {
                System.out.printf("  %-8s  %-22s  %s%n",
                        v.getVoterId(), v.getName(), v.hasVoted() ? "Yes ✓" : "No");
            }
            System.out.println();
        } catch (SQLException e) {
            System.out.println("  ✗ Error loading voters: " + e.getMessage() + "\n");
        }
    }

    private static void castVoteInteractive() {
        // Show available candidates so the voter can see their IDs
        listCandidates();

        System.out.print("  Enter your Voter ID: ");
        String voterId = sc.nextLine().trim();

        System.out.print("  Enter Candidate ID : ");
        String candidateId = sc.nextLine().trim();

        try {
            manager.castVote(voterId, candidateId);
            System.out.println("  ✓ Vote successfully cast!\n");
        } catch (VotingException e) {
            System.out.println("  ✗ Vote rejected: " + e.getMessage() + "\n");
        } catch (SQLException e) {
            System.out.println("  ✗ Database error: " + e.getMessage() + "\n");
        }
    }

    private static void exportReport() {
        System.out.print("  Enter output file path [election_results.txt]: ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) path = "election_results.txt";
        try {
            manager.exportResults(path);
            System.out.println("  ✓ Report exported successfully.\n");
        } catch (IOException e) {
            System.out.println("  ✗ Export failed: " + e.getMessage() + "\n");
        } catch (SQLException e) {
            System.out.println("  ✗ Database error: " + e.getMessage() + "\n");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Reads a positive integer from the console, re-prompting on invalid input. */
    private static int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value > 0) return value;
                System.out.println("  Please enter a number greater than 0.");
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid whole number.");
            }
        }
    }

    /** Reads a non-negative integer (0 or above) from the console, re-prompting on invalid input. */
    private static int readNonNegativeInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= 0) return value;
                System.out.println("  Please enter 0 or a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid whole number.");
            }
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

    /**
     * Checks whether an election with the given name already exists.
     * If so, asks the user whether to proceed; if yes, auto-generates a
     * unique name by appending (2), (3), etc.
     *
     * @param name the desired election name
     * @return the resolved (possibly suffixed) unique name, or {@code null}
     *         if the user chose to cancel
     */
    private static String resolveElectionName(String name) {
        try {
            ElectionRecord existing = electionDAO.findByName(name);
            if (existing == null) {
                return name;  // no duplicate
            }

            System.out.printf("%n  An election named '%s' already exists.%n", name);
            System.out.printf("  Existing Election ID: %d%n", existing.electionId);
            System.out.print("  Create another election with the same name? (y/n): ");
            String confirm = sc.nextLine().trim().toLowerCase();

            if (!confirm.equals("y") && !confirm.equals("yes")) {
                System.out.println("  Creation cancelled.\n");
                return null;
            }

            // Auto-generate a unique name: "Name (2)", "Name (3)", ...
            int suffix = 2;
            String uniqueName;
            do {
                uniqueName = name + " (" + suffix + ")";
                suffix++;
            } while (electionDAO.findByName(uniqueName) != null);

            System.out.printf("  Using unique name: '%s'%n%n", uniqueName);
            return uniqueName;
        } catch (SQLException e) {
            System.out.println("  ✗ Error checking election name: " + e.getMessage() + "\n");
            return null;
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════╗");
        System.out.println("  ║      ONLINE VOTING SYSTEM  v2.0          ║");
        System.out.println("  ║      Secure · Fair · Transparent         ║");
        System.out.println("  ╚═══════════════════════════════════════════╝");
        System.out.println();
    }
}
