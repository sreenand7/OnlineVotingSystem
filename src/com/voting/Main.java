package com.voting;

import com.voting.manager.VotingManager;
import com.voting.model.Candidate;
import com.voting.model.Voter;
import com.voting.util.VotingException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Scanner;

/**
 * Entry point for the Online Voting System.
 *
 * <p>Presents a menu-driven command-line interface that lets an operator:</p>
 * <ul>
 *   <li>Set up an election with voters and candidates.</li>
 *   <li>Cast votes interactively.</li>
 *   <li>View real-time results.</li>
 *   <li>Export a report to a text file.</li>
 * </ul>
 *
 * <p>Run: {@code java -cp out com.voting.Main}</p>
 */
public class Main {

    private static VotingManager manager;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        printBanner();
        setupElection();
        menuLoop();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Bootstraps a sample election with pre-registered voters and candidates,
     * then gives the operator the option to start the voting window right now.
     */
    private static void setupElection() {
        System.out.println("\n  Setting up the General Election 2025 demo...\n");

        // Election window: starts now, closes in 5 minutes (adjustable for demos)
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end   = start.plusMinutes(5);

        manager = new VotingManager("General Election 2025", start, end);

        // ── Register candidates ───────────────────────────────────────────────
        manager.registerCandidate(new Candidate("C001", "Alice Mercer",    "National Progress Party"));
        manager.registerCandidate(new Candidate("C002", "Bob Harrington",  "Liberty Alliance"));
        manager.registerCandidate(new Candidate("C003", "Clara Vasquez",   "Green Future Party"));
        manager.registerCandidate(new Candidate("C004", "David Chen",      "United Democrats"));

        // ── Register voters ───────────────────────────────────────────────────
        manager.registerVoter(new Voter("V001", "Ethan Walker"));
        manager.registerVoter(new Voter("V002", "Fiona Murphy"));
        manager.registerVoter(new Voter("V003", "George Kim"));
        manager.registerVoter(new Voter("V004", "Hannah Patel"));
        manager.registerVoter(new Voter("V005", "Ivan Sokolov"));
        manager.registerVoter(new Voter("V006", "Julia Santos"));
        manager.registerVoter(new Voter("V007", "Kevin O'Brien"));
        manager.registerVoter(new Voter("V008", "Laura Bianchi"));

        System.out.println("  ✓  4 candidates registered.");
        System.out.println("  ✓  8 voters registered.");
        System.out.printf ("  ✓  Voting window: %s  →  %s%n%n",
                start.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")),
                end.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // ── Menu loop ─────────────────────────────────────────────────────────────

    private static void menuLoop() {
        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> listCandidates();
                case "2" -> listVoters();
                case "3" -> castVoteInteractive();
                case "4" -> manager.displayResults();
                case "5" -> exportReport();
                case "6" -> runDemoVotes();
                case "0" -> running = false;
                default  -> System.out.println("  Invalid option. Please choose 0-6.\n");
            }
        }
        System.out.println("  Goodbye!\n");
    }

    private static void printMenu() {
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│       ONLINE VOTING SYSTEM  v1.0        │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  1. List candidates                     │");
        System.out.println("│  2. List registered voters              │");
        System.out.println("│  3. Cast a vote                         │");
        System.out.println("│  4. Display election results            │");
        System.out.println("│  5. Export results to file              │");
        System.out.println("│  6. Run demo votes (automated test)     │");
        System.out.println("│  0. Exit                                │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.print("  Choose an option: ");
    }

    // ── Option handlers ───────────────────────────────────────────────────────

    private static void listCandidates() {
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

    /**
     * Runs a scripted sequence of votes to showcase all validation paths:
     * successful votes, duplicate vote attempt, unknown voter, unknown candidate.
     */
    private static void runDemoVotes() {
        System.out.println("  ── Running automated demo votes ──\n");

        Object[][] scenarios = {
            // voterId,  candidateId,  description
            {"V001", "C001", "Valid vote: Ethan → Alice"},
            {"V002", "C003", "Valid vote: Fiona → Clara"},
            {"V003", "C002", "Valid vote: George → Bob"},
            {"V004", "C001", "Valid vote: Hannah → Alice"},
            {"V005", "C004", "Valid vote: Ivan → David"},
            {"V006", "C002", "Valid vote: Julia → Bob"},
            {"V001", "C002", "DUPLICATE: Ethan tries to vote again"},
            {"V999", "C001", "UNKNOWN VOTER: V999"},
            {"V007", "C999", "UNKNOWN CANDIDATE: C999"},
            {"V007", "C003", "Valid vote: Kevin → Clara"},
        };

        for (Object[] s : scenarios) {
            String voterId     = (String) s[0];
            String candidateId = (String) s[1];
            String desc        = (String) s[2];

            System.out.printf("  [TEST] %s%n", desc);
            try {
                manager.castVote(voterId, candidateId);
                System.out.println("         → SUCCESS\n");
            } catch (VotingException e) {
                System.out.println("         → REJECTED: " + e.getMessage() + "\n");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void printBanner() {
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════╗");
        System.out.println("  ║      ONLINE VOTING SYSTEM  v1.0          ║");
        System.out.println("  ║      Secure · Fair · Transparent         ║");
        System.out.println("  ╚═══════════════════════════════════════════╝");
        System.out.println();
    }
}
