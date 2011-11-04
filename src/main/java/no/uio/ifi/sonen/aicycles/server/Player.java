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

import no.uio.ifi.sonen.aicycles.net.Connection;
import no.uio.ifi.sonen.aicycles.net.MalformedPacketException;
import no.uio.ifi.sonen.aicycles.net.Packet;
import java.io.IOException;
import no.uio.ifi.sonen.aicycles.Direction;

/**
 *
 * @author Sigmund Hansen <sigmunha@ifi.uio.no>
 */
public class Player implements Runnable {

    /** The ID of the player in this game. */
    private int id;
    /** The name of the player. */
    private String name;
    
    /** The direction the player is travelling. */
    private volatile Direction dir = Direction.N;
    /** The horizontal position of the player. */
    private int x;
    /** The vertical position of the player. */
    private int y;
    /** Whether the player is alive or not. */
    private boolean alive = true;
    
    /** The network connection of the player. */
    private Connection con;
    /** Whether the connection was closed cleanly. */
    private boolean cleanDisconnect;

    /**
     * Creates a player with the given ID and name.
     * 
     * @param id The id of the player.
     * @param name The name of the player.
     * @param x The horizontal start position.
     * @param y The vertical start position.
     */
    public Player(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    /**
     * Sets a player's connection.
     * 
     * @param con The connection to the player client.
     */
    public void setConnection(Connection con) {
        if (this.con != null) {
            throw new IllegalStateException("Already connected!");
        }

        this.con = con;
    }

    /**
     * Gets the player's ID.
     * 
     * @return The player's ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the player's name.
     * 
     * @return The player's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the direction the player is travelling.
     * 
     * @return The direction the player is travelling.
     */
    public Direction getDir() {
        return dir;
    }

    /**
     * Moves the bike one square based on its direction.
     * 
     * @return The direction the player moved.
     */
    public Direction update() {
        Direction dir = this.dir;
        
        switch (dir) {
            case N:
                y--;
                break;
            case E:
                x++;
                break;
            case S:
                y++;
                break;
            case W:
                x--;
                break;
        }
        
        return dir;
    }

    /**
     * De-rezzes (kills) the player.
     */
    public void derez() {
        alive = false;
    }

    /**
     * Sends a packet on the player's connection.
     * 
     * @param p The packet to send.
     * @throws IOException if the connection throws an IOException.
     * @see Connection#sendPacket(no.uio.ifi.sonen.aicycles.Packet) 
     */
    public void sendPacket(Packet p) throws IOException {
        if (con.isDown()) {
            return;
        }
        
        con.sendPacket(p);
    }
    
    /**
     * Disconnects this player.
     * 
     * @see Connection#close() 
     */
    public void disconnect() {
        cleanDisconnect = true;
        con.close();
    }
    
    /**
     * Reads network packets from a client.
     */
    public void run() {
        if (con == null) {
            throw new IllegalStateException("Player is not connected");
        }

        while (true) {
            try {
                Packet p = con.receivePacket();
                if (p instanceof Packet.DirectionPacket) {
                    Packet.DirectionPacket dp = (Packet.DirectionPacket) p;
                    dir = dp.getDirection();
                } else if (p.getPacketType() == Packet.BYE_PKT) {
                    System.out.printf("%s disconnected: %s%n",
                                      name, p.getData());
                    disconnect();
                    break;
                }

            } catch (MalformedPacketException mpe) {
                System.err.printf("Malformed packet from %s%n", name);
                System.err.println(mpe.getMessage());
                
                if (con.isDown()) {
                    return;
                }

            } catch (IOException ioe) {
                if (cleanDisconnect) {
                    return;
                }
                
                System.err.printf("IO problems with #%s's connection.%n", name);
                System.err.println(ioe.getMessage());
                
                if (con.isDown()) {
                    return;
                }
            }
        }
    }

    /**
     * Gets the horizontal position of the player.
     * 
     * @return The horizontal position.
     */
    int getX() {
        return x;
    }

    /**
     * Gets the vertical position of the player.
     * 
     * @return The vertical position.
     */
    int getY() {
        return y;
    }

    /**
     * Gets whether the player is alive.
     * 
     * @return true if the player has not been de-rezzed.
     */
    boolean isAlive() {
        return alive;
    }
}
