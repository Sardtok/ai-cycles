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
package no.uio.ifi.sonen.aicycles;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * A single light cycle match with players and game state used by a simulation.
 *
 * @author Sigmund Hansen <sigmunha@ifi.uio.no>
 */
public class Match implements Runnable {

    int[][] map;
    Player[] players;

    /**
     * Creates a match with a map of the given size and the given players.
     * 
     * @param width The width of the game map.
     * @param height The hight of the game map.
     * @param players The names of the players to play with.
     */
    public Match(int width, int height, String[] players) {
        map = new int[width][height];
        this.players = new Player[players.length];
        for (int i = 0; i < players.length; i++) {
            this.players[i] = new Player(i + 1, players[i]);
        }
    }

    public void run() {
        connectPlayers();
        simulate();
    }

    private void connectPlayers() {
        int connectedPlayers = 0;
        ServerSocket ss = null;

        try {
            ss = new ServerSocket(1982);
        } catch (IOException e) {
            System.err.printf("Could not create server socket: %n%s%n",
                              e.getMessage());
            System.exit(1);
        }

        while (connectedPlayers < players.length) {
            Connection c = null;
            try {
                c = new Connection(ss.accept());
                if (connectPlayer(c)) {
                    connectedPlayers++;
                }

            } catch (MalformedPacketException mpe) {
                System.err.println("Connecting player sent malformed packet:");
                System.err.println(mpe.getMessage());
                
                if (c != null && !c.isDown()) {
                    c.close();
                }
                
            } catch (IOException ioe) {
                System.err.printf("Error connecting player: %n%s%n",
                                  ioe.getMessage());
                
                if (c != null && !c.isDown()) {
                    c.close();
                }
                
                if (ss.isClosed()) {
                    System.exit(2);
                }
            }
        }
    }

    /**
     * Tries to match a connection with a player based on its user name.
     * 
     * @param con The connection a player is trying to connect on.
     * @return true if the player was connected, false if not.
     * @throws IOException if receiving or sending packets on the connection fail,
     *                     or closing the connection upon problems fail.
     * @throws MalformedPacketException if the packet received from the client
     *                                  doesn't match any of the protocol packets.
     */
    private boolean connectPlayer(Connection con) throws IOException, MalformedPacketException {
        
        con.sendPacket(new Packet.HandshakePacket("You're in trouble now, program! Who's your user?"));
        Packet pkt = con.receivePacket();
        
        if (pkt instanceof Packet.HandshakePacket) {
            Packet.HandshakePacket np = (Packet.HandshakePacket) pkt;

            try {
                for (Player p : players) {
                    if (p.getName().equals(np.getData())) {
                        p.setConnection(con);
                        return true;
                    }
                }

            } catch (IllegalStateException ise) {
                System.err.println(ise.getMessage());
            }
        }
        
        con.close();
        return false;
    }

    private void simulate() {
    }
}
