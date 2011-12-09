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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author Sigmund Hansen <sigmund@chickensoft.com>
 */
public class Statistics {
    
    /**
     * Used to hold statistics for a team.
     */
    private class Team implements Comparable<Team> {
        /** The name of the team. */
        private String name;
        /** The start position of the player (for individual game stats). */
        private int startX;
        /** The start position of the player (for individual game stats). */
        private int startY;
        /** The number of rounds the player har survived (length of the light trail). */
        private int length;
        /** The number of points the player has scored (number of players it has survived). */
        private int points;
        
        /**
         * Creates a new team with the given start position.
         * 
         * @param x The team's start position X axis.
         * @param y The team's start position Y acis.
         * @param name The name of the team.
         */
        private Team(int x, int y, String name) {
            this.startX = x;
            this.startY = y;
            this.name = name;
        }
        
        /**
         * Sets the length of the player.
         * 
         * @param length The length to give the player.
         */
        private void setLength(int length) {
            this.length = length;
        }
        
        /**
         * Sets the points scored by the player.
         * 
         * @param points The number of points to give the player.
         */
        private void setPoints(int points) {
            this.points = points;
        }

        /**
         * Compares this player to another player based on the number of points scored.
         * In case of equal number of points, the number of rounds the player has survived breaks the tie.
         * 
         * @param t The team to compare this team to.
         * @return The difference in points or length of the teams.
         */
        public int compareTo(Team t) {
            int diff = t.points - points;
            if (diff == 0) {
                diff = t.length - length;
            }
            
            return diff;
        }
    }
    
    /** A list of maps. */
    private Map<String, Team> teams;
    /** The width of the map that was played. */
    private int width;
    /** The height of the map that was played. */
    private int height;
    /** The random seed used for the game. */
    private int randomSeed;
    /** The number of players in the game. */
    private int playerCount;
    /** The number of games played. */
    private int gameCount;
    
    /**
     * Creates a statistics object.
     * 
     * @param width The width of the map in the game these stats are for.
     * @param height The height of the map in the game these stats are for.
     * @param randomSeed The seed used in the game these stats are for.
     * @param playerCount The number of players in the game these stats are for.
     */
    public Statistics(int width, int height, int randomSeed, int playerCount) {
        this.width = width;
        this.height = height;
        this.randomSeed = randomSeed;
        this.playerCount = playerCount;
        this.teams = new LinkedHashMap<String, Team>(playerCount);
    }
    
    /**
     * Adds a team to the list of teams.
     * 
     * @param p The player to represent.
     */
    public void addTeam(Player p) {
        Team t = new Team(p.getX(), p.getY(), p.getName());
        teams.put(p.getName(), t);
    }
    
    /**
     * Adds a team based on the given values.
     * 
     * @param name The name of the team.
     * @param x The start position on the x axis.
     * @param y The start position on the y axis.
     * @param points The points scored by the team.
     * @param length The length of the team's trail.
     */
    public void addTeam(String name, int x, int y, int points, int length) {
        Team t = new Team(x, y, name);
        t.setPoints(points);
        t.setLength(length);
        teams.put(name, t);
    }
    
    /**
     * Sets the length of the given player.
     * 
     * @param p The name of the player whose length to set.
     * @param length The length to set for the player.
     */
    public void setLength(String p, int length) {
        Team t = teams.get(p);
        if (t != null) {
            t.setLength(length);
        }
    }
    
    /**
     * Sets the number of points scored by a player.
     * 
     * @param p The name of the player whose score to set.
     * @param points The number of points the player has scored.
     */
    public void setPoints(String p, int points) {
        Team t = teams.get(p);
        if (t != null) {
            t.setPoints(points);
        }
    }
    
    /**
     * Adds the other statistics to these stats.
     * 
     * @param s The stats to add to these stats.
     */
    public void add(Statistics s) {
        for (Map.Entry<String, Team> teamEntry : s.teams.entrySet()) {
            if (!teams.containsKey(teamEntry.getKey())) {
                teams.put(teamEntry.getKey(), teamEntry.getValue());
            } else {
                Team t = teams.get(teamEntry.getKey());
                t.length += teamEntry.getValue().length;
                t.points += teamEntry.getValue().points;
            }
        }
        
        gameCount++;
    }

    /**
     * Gets the teams in order of their rank.
     * 
     * @return An array of team names ordered by the teams' rank.
     */
    public String[] getRankedTeams() {
        Queue<Team> rankingList = new PriorityQueue<Team>(teams.values());
        String[] rankedNames = new String[rankingList.size()];
        int i = 0;
        while (!rankingList.isEmpty()) {
            rankedNames[i++] = rankingList.poll().name;
        }
        return rankedNames;
    }
    
    /**
     * Appends a piece of statistics to the given string builder
     * and a semicolon to separate data.
     * 
     * @param sb The string builder to append the data to.
     * @param data The data to append to the string.
     */
    private void append(StringBuilder sb, Object data) {
        sb.append(data);
        sb.append(";");
    }
    
    /**
     * Converts these stats to a string.
     * 
     * @return A string representing these stats.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        append(sb, width);
        append(sb, height);
        append(sb, randomSeed);
        append(sb, playerCount);
        
        for(Team t : teams.values()) {
            append(sb, t.name);
            append(sb, t.startX);
            append(sb, t.startY);
            append(sb, t.length);
            append(sb, t.points);
        }
        
        return sb.toString();
    }
}
