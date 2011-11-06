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
package no.uio.ifi.sonen.aicycles.net;

import java.util.Scanner;
import no.uio.ifi.sonen.aicycles.Direction;

/**
 * An abstract class for representing packets.
 *
 * @author Sigmund Hansen <sigmunha@ifi.uio.no>
 */
public abstract class Packet {
    
    /** The string representation of the packet contents. */
    protected String data;
    
    // 1XX - connection and match data
    /** Handshake packet. */
    public static final int SHK_PKT = 100;
    /** Player ID packet. */
    public static final int PID_PKT = 101;
    /** Map packet. */
    public static final int MAP_PKT = 102;
    /** Position packet. */
    public static final int POS_PKT = 103;
    /** Disconnect packet. */
    public static final int BYE_PKT = 199;
    
    // 4XX - game state changes
    /** A player moves one square. */
    public static final int MOV_PKT = 400;
    /** Set player direction. */
    public static final int DIR_PKT = 401;
    /** Packet which tells the client that all players have been updated. */
    public static final int UPD_PKT = 402;
    /** A player has crashed. */
    public static final int DIE_PKT = 404;
    
    /**
     * Gets the textual data associated with this packet.
     * 
     * @return A string representation of this packet's data.
     */
    public String getData() {
        return data;
    }
    
    /**
     * Gets the packet's type number.
     * 
     * @return The packet's type number.
     */
    public abstract int getPacketType();
    
    /**
     * A simple packet class for all packets that only have a string for data.
     */
    public static class SimplePacket extends Packet {

        /** The type of packet this represents. */
        private final int type;
        
        /**
         * Creates a simple packet.
         * 
         * @param data The string of data to send or receive.
         * @param type The type of packet to create.
         */
        public SimplePacket(String data, int type) {
            this.data = data;
            this.type = type;
        }
        
        /** {@inheritDoc} */
        @Override
        public int getPacketType() {
            return type;
        }
    }
    
    public static class IntPacket extends Packet {

        private final int intValue;
        private final int type;
        
        public IntPacket(int intValue, int type) {
            this.intValue = intValue;
            this.type = type;
            this.data = Integer.toString(intValue);
        }
        
        public IntPacket(String data, int type) {
            this.intValue = Integer.parseInt(data);
            this.type = type;
            this.data = data;
        }
        
        public int getIntValue() {
            return intValue;
        }
        
        @Override
        public int getPacketType() {
            return type;
        }
        
    }
    
    /**
     * A packet describing player moves in the game.
     */
    public static class MovePacket extends Packet {
        
        /** The ID of the player that moved. */
        private int player;
        /** The direction the player has moved. */
        private Direction dir;
        
        /**
         * Creates a move packet from a player ID and direction.
         * 
         * @param player The player's ID.
         * @param dir The direction the player moved.
         */
        public MovePacket(int player, Direction dir) {
            this.player = player;
            this.dir = dir;
            this.data = String.format("%d %s", player, dir.name());
        }
        
        /**
         * Creates a move packet from a string.
         * 
         * @param data A string containing the player's ID and the direction.
         */
        public MovePacket(String data) {
            this.data = data;
            Scanner s = new Scanner(data);
            this.player = s.nextInt();
            this.dir = Direction.valueOf(s.next());
            if (this.dir == null) {
                throw new IllegalArgumentException(String.format("Not a direction: %s", data));
            }
        }
        
        /**
         * Gets the direction set in the packet.
         * 
         * @return The direction represented in this packet.
         */
        public Direction getDirection() {
            return dir;
        }
        
        /**
         * Gets the player who moved.
         * 
         * @return The player that moved.
         */
        public int getPlayer() {
            return player;
        }

        /** {@inheritDoc} */
        @Override
        public int getPacketType() {
            return MOV_PKT;
        }
    }
    
    /**
     * A packet describing direction changes for players.
     */
    public static class DirectionPacket extends Packet {
        
        /** The new direction. */
        private Direction dir;
        
        /**
         * Creates a new direction packet from the given direction.
         * 
         * @param dir The direction to change into.
         */
        public DirectionPacket(Direction dir) {
            this.dir = dir;
            this.data = dir.name();
        }
        
        /**
         * Creates a new direction packet from a string.
         * 
         * @param data A string containing the direction to change into.
         */
        public DirectionPacket(String data) {
            this.dir = Direction.valueOf(data);
            this.data = data;
            if (this.dir == null) {
                throw new IllegalArgumentException(String.format("Not a direction: %s", data));
            }
        }
        
        /**
         * Gets the direction in which to move.
         * 
         * @return The direction associated with this packet.
         */
        public Direction getDirection() {
            return dir;
        }
        
        /** {@inheritDoc} */
        public int getPacketType() {
            return DIR_PKT;
        }
    }
    
    /**
     * A packet describing a player's position.
     */
    public static class PositionPacket extends Packet {
        
        /** The ID of the player. */
        private int player;
        /** The horizontal position. */
        private int x;
        /** The vertical position. */
        private int y;
        
        /**
         * Creates a position packet from a player ID and X and Y coordinates.
         * 
         * @param player The ID of the player.
         * @param x The player's horizontal position.
         * @param y The player's vertical position.
         */
        public PositionPacket(int player, int x, int y) {
            this.player = player;
            this.x = x;
            this.y = y;
            this.data = String.format("%d %d %d", player, x, y);
        }
        
        /**
         * Creates a position packet from a string.
         * 
         * @param data A string containing the ID of a player and its position.
         */
        public PositionPacket(String data) {
            Scanner s = new Scanner(data);
            this.player = s.nextInt();
            this.x = s.nextInt();
            this.y = s.nextInt();
            this.data = data;
        }

        /**
         * Gets the horizontal position.
         * 
         * @return The horizontal position.
         */
        public int getX() {
            return x;
        }
        
        /**
         * Gets the vertical position.
         * 
         * @return The vertical position.
         */
        public int getY() {
            return y;
        }
        
        /**
         * Gets the player whose position is contained in this packet.
         * 
         * @return The player placed at these coordinates.
         */
        public int getPlayer() {
            return player;
        }
        
        /** {@inheritDoc} */
        @Override
        public int getPacketType() {
            return POS_PKT;
        }
    }
    
    /**
     * A packet describing a map's size and number of players.
     */
    public static class MapPacket extends Packet {
        
        /** The number of players. */
        private int players;
        /** The width of the map. */
        private int width;
        /** The height of the map. */
        private int height;
        
        /**
         * Creates a map packet from the given width, height and number of players.
         * 
         * @param width The width of the map.
         * @param height The height of the map.
         * @param players The number of players.
         */
        public MapPacket(int width, int height, int players) {
            this.width = width;
            this.height = height;
            this.players = players;
            this.data = String.format("%d %d %d", width, height, players);
        }
        
        /**
         * Creates a map packet from a string.
         * 
         * @param data A string containing the width, height and number of players.
         */
        public MapPacket(String data) {
            Scanner s = new Scanner(data);
            this.width = s.nextInt();
            this.height = s.nextInt();
            this.players = s.nextInt();
            this.data = data;
        }

        /**
         * Gets the width of the game area.
         * 
         * @return The width of the game area.
         */
        public int getWidth() {
            return width;
        }
        
        /**
         * Gets the height of the game area.
         * 
         * @return The height of the game area.
         */
        public int getHeight() {
            return height;
        }
        
        /**
         * Gets the number of players in the current game.
         * 
         * @return The number of players.
         */
        public int getPlayers() {
            return players;
        }
        
        /** {@inheritDoc} */
        @Override
        public int getPacketType() {
            return MAP_PKT;
        }
    }
}
