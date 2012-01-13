/*
 * Copyright (c) 2011, Åpen sone for eksperimentell informatikk
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of the <organization> nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.uio.ifi.sonen.aicycles.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import no.uio.ifi.sonen.aicycles.Viewer;

/**
 * AICycles simulator/server.
 * 
 * @author Sigmund Hansen <sigmunha@ifi.uio.no>
 */
public class Server implements Runnable {

    /** Statistics for an entire tournament. */
    private Statistics totalStats = new Statistics(0, 0, 0, 0);
    /** The name of the file containing statistics. */
    private String statsFile;
    /** A list of teams that participate in the tournament. */
    private List<String> teams = new LinkedList<String>();
    /** A list of rounds to be played. */
    private List<Round> rounds = new LinkedList<Round>();
    /** The round currently being played. */
    private Round currentRound;

    /**
     * Creates a runnable tournament server.
     * 
     * @param configFile The name of the configuration file.
     * @param statsFile 
     */
    public Server(String configFile, String statsFile) {
        this.statsFile = statsFile;

        try {
            Scanner s = new Scanner(new File(configFile));
            int teamCount = s.nextInt();
            for (int i = 0; i < teamCount; i++) {
                String t = s.next();
                teams.add(t);
                System.out.println("Adding team: " + t);
            }
            s.useDelimiter("[;\\s]+");
            while (s.hasNext()) {
                System.out.println("Adding round.");
                int w = s.nextInt();
                System.out.println(w);
                int h = s.nextInt();
                System.out.println(h);
                int p = s.nextInt();
                System.out.println(p);
                int pR = s.nextInt();
                System.out.println(pR);
                System.out.printf("Width: %d Height: %d Players: %d Teams: %d%n", w, h, p, pR);
                rounds.add(new Round(new MatchSize(w, h, p),
                                     pR));
            }

        } catch (IOException ioe) {
            System.err.printf("Couldn't open configuration file '%s':%n%s%n",
                              configFile, ioe.getMessage());
            System.exit(13);
        } catch (Exception e) {
            System.err.printf("Bad format in configuration file:%n%s%n",
                              e.getMessage());
            System.exit(17);
        }

        try {
            currentRound = rounds.remove(0);
            Scanner s = new Scanner(new File(statsFile));
            s.useDelimiter("[;\\s]+");
            while (s.hasNext()) {
                int playerCount = 0;
                Statistics stats = new Statistics(s.nextInt(), s.nextInt(),
                                                  s.nextInt(), playerCount = s.nextInt());
                for (int i = 0; i < playerCount; i++) {
                    stats.addTeam(s.next(), s.nextInt(), s.nextInt(),
                                  s.nextInt(), s.nextInt());
                }
                
                totalStats.add(stats);
                advance();
            }
        } catch (IOException ioe) {
            System.err.printf("Couldn't open statistics file '%s':%n%s%nStarting from first round.",
                              statsFile, ioe.getMessage());
        } catch (Exception e) {
            System.err.printf("Bad format in statistics file:%n%s%n",
                              e.getMessage());
            System.exit(21);
        }
    }

    /**
     * Entry point.
     * 
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Hello, bots!");
            Viewer v = new Viewer(false);
            Match m = new Match(47, 47, (int) (Math.random() * Integer.MAX_VALUE),
                                new String[]{"joe", "sigmunha"}, v);
            m.run();
        } else {
            Server s = new Server(args[0], args[1]);
            System.out.println("Ready to go!");
            s.run();
        }
    }

    /**
     * Runs through all the matches in the tournament
     * displayed in a fullscreen viewer.
     */
    public void run() {
        String[] matchTeams;
        Viewer v = new Viewer(true);
        
        while ((matchTeams = getMatch()) != null) {
            Match m = new Match(currentRound.size.width, currentRound.size.height,
                                (int) (Math.random() * Integer.MAX_VALUE), matchTeams, v);
            m.run();
            Statistics s = m.getStatistics();
            totalStats.add(s);
            writeStats(s);
            advance();
        }
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            
        }
        
        v.close();
    }
    
    /**
     * Writes the given statistics to the statistics file.
     * 
     * @param s The statistics to write to the file.
     */
    private void writeStats(Statistics s) {
            try {
                PrintWriter pw = new PrintWriter(new FileOutputStream(statsFile, true));
                pw.println(s);
                pw.close();
            } catch (FileNotFoundException ex) {
                System.err.printf("Could not find statistics file: %s%n%s%n", statsFile, ex.getMessage());
                System.exit(74);
            }
    }
    
    /**
     * Advances to the next round in the tournament.
     */
    private void advance() {
        if (!currentRound.finishMatch()) {
            return;
        }
        
        if (rounds.isEmpty()) {
            currentRound = null;
            return;
        }
        
        String[] ranking = totalStats.getRankedTeams();
        currentRound = rounds.remove(0);
        teams.clear();
        
        // This keeps only the best teams.
        for (int i = 0; i < currentRound.playerCount; i++) {
            teams.add(ranking[i]);
        }
    }
    
    /**
     * Gets the next match to play.
     * 
     * @return The teams in the next match or null if there are no more matches.
     */
    private String[] getMatch() {
        if (currentRound == null)
            return null;
        
        return currentRound.getMatch(currentRound.finishedMatches, teams);
    }
    
    /**
     * A small structure to hold the width, height and number of players
     * a match in a given round should have.
     */
    private class MatchSize {

        /** The width of the map. */
        public final int width;
        /** The height of the map. */
        public final int height;
        /** The number of players in each match. */
        public final int players;

        /**
         * Creates a MatchSize object.
         * 
         * @param width The width.
         * @param height The height.
         * @param players The number of players.
         */
        public MatchSize(int width, int height, int players) {
            this.width = width;
            this.height = height;
            this.players = players;
        }
    }

    /**
     * Used to handle rounds in the tournament,
     * mainly setting up permutations of teams for each match in a round.
     */
    private class Round {

        /** The size of matches one round. */
        private MatchSize size;
        /** A list of all matches and their permutations. */
        private int[][] matches;
        /** The number of players left in the tournament. */
        private int playerCount;
        /** The number of matches that have been finished in this round. */
        private int finishedMatches = 0;

        /**
         * Creates a round for the tournament.
         * 
         * @param size The size of each match.
         * @param playerCount The number of players left in the tournament.
         */
        public Round(MatchSize size, int playerCount) {
            this.size = size;
            this.playerCount = playerCount;

            int factN = getFactorial(playerCount);
            int factK = getFactorial(size.players);
            int factNK = getFactorial(playerCount - size.players);
            matches = new int[factN / (factK * factNK)][size.players];
            setCombinations();
        }
        
        /**
         * Marks another match as finished and returns true if the round is over.
         * 
         * @return true if there are no more matches in this round.
         */
        private boolean finishMatch() {
            return ++finishedMatches >= matches.length;
        }

        /**
         * Non-recursive base for starting the recursive function
         * to set the first element in the combination.
         * 
         * @see Round#setCombinations(int, int, int) 
         */
        private void setCombinations() {
            int index = 0;
            for (int i = 0; i < size.players && index < matches.length; i++) {
                index = setCombinations(i, index, 0);
            }
        }

        /**
         * Recursive function to set up combinations.
         * Each recursive call is responsible for setting up one level of K
         * in a combination of length K.
         * 
         * @param offset How far into the possible indices to start looking.
         * @param index The number of the current combination being set up.
         * @param depth How deep into the combination we are.
         * @return The index of the combination,
         *         which is increased whenever we get to the last element.
         */
        private int setCombinations(int offset, int index, int depth) {
            if (offset >= size.players
                || index >= matches.length
                || depth >= size.players) {
                return index;
            }

            if (depth == matches[index].length - 1) {
                int i = index + 1;
                matches[index][depth] = offset;
                if (i < matches.length) {
                    // Copy all but the last element
                    for (int j = 0; j < depth; j++) {
                        matches[i][j] = matches[index][j];
                    }
                }
                return i;
            }

            for (int i = offset + 1; i < size.players && index < matches.length; i++) {
                matches[index][depth] = offset;
                index = setCombinations(i, index, depth + 1);
            }

            return index;
        }

        /**
         * Calculates and gets factorials.
         * 
         * @param n The number to get the factorial for.
         * @return The factorial for n.
         */
        private int getFactorial(int n) {
            int fact = 1;
            for (int i = 1; i <= n; i++) {
                fact *= i;
            }
            return fact;
        }

        /**
         * Sets up a permutation of player combination n, using n as a lehmer code.
         * 
         * @param n The match number, player combination index and lehmer code.
         * @param players The list of players still in the tournament.
         * @return An array containing the names of the players
         *         for the next game in the correct order.
         */
        private String[] getMatch(int n, List<String> players) {
            if (n > matches.length) {
                return null;
            }

            int[] combo = new int[size.players];
            String[] match = new String[size.players];
            List<String> p = new LinkedList<String>(players);

            // Convert decimal to factoradic digits
            for (int i = 1; i <= combo.length; i++) {
                combo[i - 1] = n % i;
                n /= i;
            }

            for (int i = match.length - 1; i >= 0; i--) {
                int index = matches[n][p.size() - combo[i] - 1];
                match[i] = p.remove(index);
            }

            return match;
        }
    }
}
