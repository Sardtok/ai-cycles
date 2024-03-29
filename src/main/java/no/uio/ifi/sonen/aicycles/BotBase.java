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
import java.net.Socket;
import java.util.Random;

import no.uio.ifi.sonen.aicycles.net.Connection;
import no.uio.ifi.sonen.aicycles.net.MalformedPacketException;
import no.uio.ifi.sonen.aicycles.net.Packet;

/**
 * A base class for creating bots.
 * This will handle the network connection and update the map and players.
 *
 * @author Sigmund Hansen <sigmund@chickensoft.com>
 */
public abstract class BotBase {
    
    /** The connection the client is using. */
    private Connection con;
    /** The number of updates. */
    protected int updates = 0;
    /** This bot's ID. */
    protected int id;
    /** All the players in a game. */
    protected Cycle[] cycles;
    /** The map. */
    protected int[][] map;
    /** Whether the bot has started running or not. */
    protected boolean running = false;
    /** Random number generator to be used. */
    protected Random random = new Random();
    
    /**
     * Creates and connects a bot to the server.
     * 
     * @param server The address of the server to connect to.
     */
    public BotBase(String server) {
        try {
            con = new Connection(new Socket(server, Connection.PORT));
            Packet p = con.receivePacket();
            if (p == null || p.getPacketType() != Packet.SHK_PKT) {
                System.err.println("Master Control Program wouldn't say hello!");
                System.exit(3);
            }
            con.sendPacket(new Packet.SimplePacket(getName(), Packet.SHK_PKT));
            
            p = con.receivePacket();
            if (p.getPacketType() != Packet.PID_PKT) {
                con.close();
                System.exit(4);
            }
            this.id = ((Packet.IntPacket)p).getIntValue();
            
            Packet.MapPacket mp = (Packet.MapPacket) con.receivePacket();
            map = new int[mp.getWidth() + 2][mp.getHeight() + 2];
            for (int i = 0; i < map.length; i++) {
                map[i][0] = -1;
                map[i][map[i].length - 1] = -1;
            }
            
            for (int i = 0; i < map[0].length; i++) {
                map[0][i] = -1;
                map[map.length - 1][i] = -1;
            }
            
            Packet.IntPacket ip = (Packet.IntPacket) con.receivePacket();
            random.setSeed(ip.getIntValue());
            
            cycles = new Cycle[mp.getPlayers()];
            for (int i = 0; i < cycles.length; i++) {
                Packet.PositionPacket pp = (Packet.PositionPacket) con.receivePacket();
                cycles[pp.getPlayer() - 1] = new Cycle(pp.getX() + 1, pp.getY() + 1);
                map[pp.getX() + 1][pp.getY() + 1] = pp.getPlayer();
            }
            
        } catch (IOException ioe) {
            System.err.printf("Could not connect to server %s:%d%n%s%n",
                              server, Connection.PORT, ioe.getMessage());
            System.exit(1);
        } catch (MalformedPacketException mpe) {
            System.err.printf("Malformed packet from server: %n%s%n",
                              mpe.getMessage());
            System.exit(2);
        }
    }
    
    /**
     * Starts a thread that listens to the server for updates.
     */
    public void start() {
        if (running) {
            return;
        }
        
        new Thread(new StateUpdater()).start();
        running = true;
    }

    /**
     * Sets the direction of the cycle.
     * 
     * @param d The direction to move in.
     */
    protected final void setDirection(Direction d) {
        try {
            con.sendPacket(new Packet.DirectionPacket(d));
        } catch (Exception e) {
            System.err.printf("MCP won't listen:%n%s%n",
                              e.getMessage());
        }
    }
    
    /**
     * Turn this bot to the left.
     */
    protected final void turnLeft() {
        Direction d = cycles[id - 1].getDirection();
        Direction[] values = Direction.values();
        int num = (d.ordinal() + 3) % values.length;
        try {
            con.sendPacket(new Packet.DirectionPacket(values[num]));
        } catch (Exception e) {
            System.err.printf("MCP won't listen:%n%s%n",
                              e.getMessage());
        }
    }
    
    /**
     * Turns this bot to the right.
     */
    protected final void turnRight() {
        Direction d = cycles[id - 1].getDirection();
        Direction[] values = Direction.values();
        int num = (d.ordinal() + 1) % values.length;
        try {
            con.sendPacket(new Packet.DirectionPacket(values[num]));
        } catch (Exception e) {
            System.err.printf("MCP won't listen:%n%s%n",
                              e.getMessage());
        }
    }

    /**
     * Gets this bot's name.
     * 
     * @return this bot's name.
     */
    public abstract String getName();
    
    /**
     * Used for a thread that updates the state this bot sees
     * (listens to packets from the server).
     * 
     * It will also notify the bot whenever all bots have been updated.
     */
    private class StateUpdater implements Runnable {

        /**
         * Listens for packets from the server updating the state.
         */
        public void run() {
            while (!con.isDown()) {
                try {
                    Packet p = con.receivePacket();
                    switch (p.getPacketType()) {
                        case Packet.MOV_PKT:
                            Packet.MovePacket mp = (Packet.MovePacket) p;
                            Cycle c = cycles[mp.getPlayer() - 1];
                            c.setDirection(mp.getDirection());
                            c.update();
                            map[c.getX()][c.getY()] = mp.getPlayer();
                            break;
                            
                        case Packet.BYE_PKT:
                            con.sendPacket(p);
                            con.close();
                            cycles[id - 1].kill();
                            synchronized(BotBase.this) {
                                updates++;
                                BotBase.this.notify();
                            }
                            break;
                            
                        case Packet.DIE_PKT:
                            Packet.IntPacket dp = (Packet.IntPacket) p;
                            cycles[dp.getIntValue() - 1].kill();
                            break;
                            
                        case Packet.UPD_PKT:
                            synchronized(BotBase.this) {
                                updates++;
                                BotBase.this.notify();
                            }
                            break;
                    }
                    
                } catch (IOException ioe) {
                    System.err.printf("Connection issues:%n%s%n",
                                      ioe.getMessage());
                    con.close();
                    cycles[id - 1].kill();
                    synchronized (BotBase.this) {
                        updates++;
                        BotBase.this.notify();
                    }

                } catch (MalformedPacketException mpe) {
                    System.err.printf("Master Control Program is talking gibberish:%n%s%n",
                                      mpe.getMessage());
                    con.close();
                    cycles[id - 1].kill();
                    synchronized (BotBase.this) {
                        updates++;
                        BotBase.this.notify();
                    }
                }
            }
        }
    }
}
