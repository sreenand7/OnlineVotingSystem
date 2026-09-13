# Online Voting System

An Online Voting System developed in Java as a college project. The project demonstrates Object-Oriented Programming concepts along with database connectivity, DAO architecture, and a graphical user interface using Java Swing.

The system is designed to manage elections, voters, candidates, voting, and election results while keeping election data stored persistently in a MySQL database.

## About the Project

The application allows an election to be created with a specified voting duration. Voters and candidates can then be registered for that election. Once the election starts, registered voters can cast their vote, with the system ensuring that a voter cannot vote more than once in the same election.

After voting ends, the system can calculate and display the results, including the number of votes received by each candidate, voter turnout, and the winning candidate. Results can also be exported to a text file.

The system also keeps previous elections in the database, allowing their results and details to be viewed later. A completed election can be used to conduct a re-election without modifying the original election records.

## Features

### Election Management
- Create new elections
- Set the duration of an election
- Start and end elections
- View election status
- View election details
- View previous elections
- Conduct re-elections

### Voter Management
- Register voters
- Assign voters to an election
- Authenticate voters
- Track voting status
- Prevent duplicate voting

### Candidate Management
- Register candidates
- Store candidate names and political parties
- View candidates participating in an election

### Voting & Results
- Allow voting only during the active election period
- Cast votes for registered candidates
- Prevent a voter from voting more than once
- Count votes automatically
- Display election results
- Calculate voter turnout
- Display the winning candidate
- Export results to a text file

### Graphical Interface

The project includes a desktop GUI built with Java Swing. The GUI follows the same basic workflow as the original command-line version while providing a simpler and more visual way to manage elections and cast votes.

The interface uses a simple dark theme with separate screens for election management, voters, candidates, voting, results, previous elections, and re-election.

## Technologies Used

- **Java 17+** — Core programming language and OOP
- **Java Swing / AWT** — Graphical user interface
- **JDBC** — Communication between Java and MySQL
- **MySQL** — Persistent database
- **MySQL Connector/J** — JDBC driver
- **Git & GitHub** — Version control

## Project Architecture

The project uses a layered structure to keep the user interface, business logic, and database operations separate.

```text
Swing GUI / CLI
      ↓
VotingManager
      ↓
DAO Layer
      ↓
JDBC
      ↓
MySQL