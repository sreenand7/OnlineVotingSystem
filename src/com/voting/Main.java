package com.voting;
 
import com.voting.manager.VotingManager;
import com.voting.model.Candidate;
import com.voting.model.Voter;
import com.voting.util.VotingException;
 
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
 
/**
 * Entry point for the Online Voting System.
 *
 * Provides a menu-driven command-line interface with a phased workflow:
 *   Phase 1 — Configure election (name + duration)
 *   Phase 2 — Register candidates and voters
 *   Phase 3 — Start election, cast votes
 *   Phase 4 — Display / export results
 *
 * Run using:
 * java -cp output com.voting.Main
 */

public class Main {
 
    private static VotingManager manager;
    private static final Scanner sc = new Scanner(System.in);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
 
    public static void main(String[] args) {
        printBanner();
        setupElection();          // Phase 1: configure name + duration
        registrationPhase();      // Phase 2: add candidates & voters
        votingPhase();            // Phase 3: start election, cast votes
        resultsPhase();           // Phase 4: display / export
        System.out.println("  Goodbye!\n");
    }
 
    // ═══════════════════════════════════════════════════════════════════════════
    //  Phase 1 — Configure election
    // ═══════════════════════════════════════════════════════════════════════════
 
    /**
     * Prompts the operator for election name and voting-window duration.
     * The timer does NOT start yet — that happens in Phase 3.
     */
    private static void setupElection() {
        System.out.println("\n  ── Phase 1: Election Setup ──\n");
 
        System.out.print("  Enter election name: ");
        String name = sc.nextLine().trim();
        while (name.isEmpty()) {
            System.out.print("  Election name cannot be empty. Enter election name: ");
            name = sc.nextLine().trim();
        }
 
        int minutes = readPositiveInt("  Voting window duration in minutes: ");
 
        manager = new VotingManager(name, minutes);
 
        System.out.printf("%n  ✓  Election '%s' configured (%d-minute window).%n", name, minutes);
        System.out.println("  ✓  Timer will start when you choose \"Start Election\".\n");
    }
 
    // ═══════════════════════════════════════════════════════════════════════════
    //  Phase 2 — Registration
    // ═══════════════════════════════════════════════════════════════════════════
 
    /**
     * Loops over a registration-only menu until the operator explicitly
     * starts the election (option 5).
     */
    private static void registrationPhase() {
        System.out.println("  ── Phase 2: Registration ──\n");
 
        boolean registering = true;
        while (registering) {
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
                        registering = false;
                    }
                }
                case "0" -> {
                    System.out.println("  Election cancelled.\n");
                    System.exit(0);
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
        System.out.println("│  0. Cancel & exit                       │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }
 
    /**
     * Validates that at least one candidate and one voter exist,
     * then confirms and starts the election timer.
     */
    private static boolean confirmStartElection() {
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
            manager.startElection();
            System.out.printf("%n  ✓  Election started!%n");
            System.out.printf("  ✓  Voting window: %s  →  %s%n%n",
                    manager.getElectionStart().format(TIME_FMT),
                    manager.getElectionEnd().format(TIME_FMT));
            return true;
        }
        System.out.println("  Start cancelled — continue registering.\n");
        return false;
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
            if (!manager.isElectionOpen()) {
                System.out.println("  ⏰  Election voting window has ended.\n");
                break;
            }
 
            printVotingMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> castVoteInteractive();
                case "2" -> listCandidates();
                case "3" -> listVoters();
                case "4" -> manager.displayResults();
                case "5" -> {
                    System.out.println("  Ending voting phase.\n");
                    voting = false;
                }
                default  -> System.out.println("  Invalid option. Please choose 1-5.\n");
            }
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
        manager.displayResults();
 
        boolean viewing = true;
        while (viewing) {
            printResultsMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> manager.displayResults();
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
        System.out.println("│  0. Exit                                │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }
 
    // ═══════════════════════════════════════════════════════════════════════════
    //  Shared option handlers
    // ═══════════════════════════════════════════════════════════════════════════
 
    private static void addCandidate() {
        System.out.print("  Enter Candidate ID    : ");
        String id = sc.nextLine().trim();
        System.out.print("  Enter Candidate Name  : ");
        String name = sc.nextLine().trim();
        System.out.print("  Enter Political Party : ");
        String party = sc.nextLine().trim();
 
        try {
            manager.registerCandidate(new Candidate(id, name, party));
            System.out.println("  ✓ Candidate registered.\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✗ Could not register candidate: " + e.getMessage() + "\n");
        }
    }
 
    private static void addVoter() {
        System.out.print("  Enter Voter ID   : ");
        String id = sc.nextLine().trim();
        System.out.print("  Enter Voter Name : ");
        String name = sc.nextLine().trim();
 
        try {
            manager.registerVoter(new Voter(id, name));
            System.out.println("  ✓ Voter registered.\n");
        } catch (IllegalArgumentException e) {
            System.out.println("  ✗ Could not register voter: " + e.getMessage() + "\n");
        }
    }
 
    private static void listCandidates() {
        if (manager.getCandidates().isEmpty()) {
            System.out.println("  No candidates registered yet.\n");
            return;
        }
        System.out.println("  Registered Candidates:");
        System.out.println("  " + "-".repeat(54));
        System.out.printf ("  %-8s  %-22s  %s%n", "ID", "Name", "Party");
        System.out.println("  " + "-".repeat(54));
        for (Candidate c : manager.getCandidates()) {
            System.out.printf("  %-8s  %-22s  %s%n",
                    c.getCandidateId(), c.getName(), c.getPoliticalParty());
        }
        System.out.println();
    }
 
    private static void listVoters() {
        if (manager.getVoters().isEmpty()) {
            System.out.println("  No voters registered yet.\n");
            return;
        }
        System.out.println("  Registered Voters:");
        System.out.println("  " + "-".repeat(46));
        System.out.printf ("  %-8s  %-22s  %s%n", "ID", "Name", "Voted?");
        System.out.println("  " + "-".repeat(46));
        for (Voter v : manager.getVoters()) {
            System.out.printf("  %-8s  %-22s  %s%n",
                    v.getVoterId(), v.getName(), v.hasVoted() ? "Yes ✓" : "No");
        }
        System.out.println();
    }
 
    private static void castVoteInteractive() {
        System.out.print("  Enter your Voter ID: ");
        String voterId = sc.nextLine().trim();
 
        System.out.print("  Enter Candidate ID : ");
        String candidateId = sc.nextLine().trim();
 
        try {
            manager.castVote(voterId, candidateId);
            System.out.println("  ✓ Vote successfully cast!\n");
        } catch (VotingException e) {
            System.out.println("  ✗ Vote rejected: " + e.getMessage() + "\n");
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
 
    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════╗");
        System.out.println("  ║      ONLINE VOTING SYSTEM  v1.0          ║");
        System.out.println("  ║      Secure · Fair · Transparent         ║");
        System.out.println("  ╚═══════════════════════════════════════════╝");
        System.out.println();
    }
}
