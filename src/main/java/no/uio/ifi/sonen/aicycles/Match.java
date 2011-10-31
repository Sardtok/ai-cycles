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
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A single light cycle match with players and game state used by a simulation.
 *
 * @author Sigmund Hansen <sigmunha@ifi.uio.no>
 */
public class Match implements Runnable {

    private int[][] map;
    private Player[] players;
    private boolean finished = false;
    private static final long TIMESTEP = 100;
    private ConcurrentLinkedQueue<Packet> broadcastQueue =
            new ConcurrentLinkedQueue<Packet>();

    /**
     * Creates a match with a map of the given size and the given players.
     * 
     * @param width The width of the game map.
     * @param height The hight of the game map.
     * @param players The names of the players to play with.
     */
    public Match(int width, int height, String[] players) {
        map = new int[width][height];
        int dX = width / (players.length / 2 + 1);
        int dY = height / 3;
        this.players = new Player[players.length];

        for (int i = 0; i < players.length; i++) {
            int x = dX * (i % (players.length / 2) + 1);
            int y = dY * (i / 2 + 1);
            this.players[i] = new Player(i + 1, players[i], x, y);
            map[x][y] = i + 1;
        }
    }

    public void run() {
        connectPlayers();

        // A thread handling packets that should be broadcast to every user.
        new Thread(new Runnable() {

            /**
             * Checks the broadcast packet queue for packets.
             * If there are none, it sleeps for a short while and tries again.
             */
            public void run() {
                while (!finished) {
                    if (!broadcastQueue.isEmpty()) {
                        Packet pkt = broadcastQueue.poll();
                        for (Player p : players) {
                            try {
                                p.sendPacket(pkt);
                            } catch (IOException ioe) {
                                System.err.printf("Connection problems for %s:%n%s%n",
                                                  p.getName(), ioe.getMessage());
                                p.disconnect();
                            }
                        }

                    } else {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        }).start();

        for (Player p : players) {
            new Thread(p).start();
        }

        simulate();

        for (Player p : players) {
            try {
                p.sendPacket(new Packet.SimplePacket("End of line!",
                                                     Packet.BYE_PKT));
                p.disconnect();
            } catch (IOException ioe) {
                System.err.printf("Error disconnecting %s:%n%s%n",
                                  p.getName(), ioe.getMessage());
            }
        }
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

        con.sendPacket(new Packet.SimplePacket("You're in trouble now, program! Who's your user?",
                                               Packet.SHK_PKT));
        Packet pkt = con.receivePacket();

        if (pkt.getPacketType() == Packet.SHK_PKT) {
            try {
                for (Player p : players) {
                    if (p.getName().equals(pkt.getData())) {
                        p.setConnection(con);
                        System.out.printf("%s connected.%n", p.getName());
                        return true;
                    }
                }

            } catch (IllegalStateException ise) {
                System.err.println(ise.getMessage());
            }
        }

        System.out.println("Unidentified program on the game grid!");
        con.close();
        return false;
    }

    private void kill(final Player p) {
        p.derez();
        broadcastQueue.offer(new Packet.DiePacket(p.getId()));
    }

    private void move(final Player p) {
        final Direction d = p.update();
        broadcastQueue.offer(new Packet.MovePacket(p.getId(), d));
    }

    private void simulate() {
        int liveCount = players.length;
        long lastUpdate = System.nanoTime();

        while (liveCount > 1) {
            if ((System.nanoTime() - lastUpdate) / 1000000 < TIMESTEP) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }
                continue;
            }

            for (Player p : players) {
                if (!p.isAlive()) {
                    continue;
                }

                move(p);
                int x = p.getX();
                int y = p.getY();

                if (x >= 0 && x < map.length
                    && y >= 0 && y <= map[x].length
                    && map[x][y] == 0) {
                    map[x][y] = p.getId();

                } else {
                    for (Player p2 : players) {
                        if (p2 == p || !p.isAlive()) {
                            continue;
                        }

                        if (p2.getX() == x && p2.getY() == y) {
                            kill(p2);
                            liveCount--;
                        }
                    }
                    kill(p);
                    liveCount--;
                }
            }

            lastUpdate = System.nanoTime();
        }
    }
}
