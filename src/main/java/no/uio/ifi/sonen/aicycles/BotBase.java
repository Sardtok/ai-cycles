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
    /** This bot's ID. */
    protected int id;
    /** All the players in a game. */
    protected Cycle[] cycles;
    /** The map. */
    protected boolean[][] map;
    /** Whether the bot has started running or not. */
    protected boolean running = false;
    
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
            System.out.println(p);
            if (p.getPacketType() != Packet.PID_PKT) {
                con.close();
                System.exit(4);
            }
            this.id = ((Packet.IdPacket)p).getId();
            
            Packet.MapPacket mp = (Packet.MapPacket) con.receivePacket();
            map = new boolean[mp.getWidth() + 2][mp.getHeight() + 2];
            for (int i = 0; i < map.length; i++) {
                map[i][0] = true;
                map[i][map[i].length - 1] = true;
            }
            
            for (int i = 0; i < map[0].length; i++) {
                map[0][i] = true;
                map[map.length - 1][i] = true;
            }
            
            cycles = new Cycle[mp.getPlayers()];
            for (int i = 0; i < cycles.length; i++) {
                Packet.PositionPacket pp = (Packet.PositionPacket) con.receivePacket();
                cycles[pp.getPlayer() - 1] = new Cycle(pp.getX(), pp.getY());
                map[pp.getX()][pp.getY()] = true;
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
     * 
     * @return 
     */
    public abstract String getName();
    
    /**
     * 
     */
    private class StateUpdater implements Runnable {

        /**
         * 
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
                            map[c.getX()][c.getY()] = true;
                            if (mp.getPlayer() == id) {
                                synchronized(BotBase.this) {
                                    BotBase.this.notify();
                                }
                            }
                            break;
                            
                        case Packet.BYE_PKT:
                            con.sendPacket(p);
                            con.close();
                            cycles[id - 1].kill();
                            break;
                            
                        case Packet.DIE_PKT:
                            Packet.DiePacket dp = (Packet.DiePacket) p;
                            cycles[dp.getPlayer() - 1].kill();
                            break;
                    }
                    
                } catch (IOException ioe) {
                    System.err.printf("Connection issues:%n%s%n",
                                      ioe.getMessage());
                    con.close();
                } catch (MalformedPacketException mpe) {
                    System.err.printf("Master Control Program is talking gibberish:%n%s%n",
                                      mpe.getMessage());
                }
            }
        }
    }
}
