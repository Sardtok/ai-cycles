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

import java.util.Scanner;

/**
 * An abstract class for representing packets.
 *
 * @author Sigmund Hansen <sigmunha@ifi.uio.no>
 */
public abstract class Packet {
    
    /** The string representation of the packet contents. */
    protected String data;
    
    // 4XX - game state changes
    /** A player moves one square. */
    public static final int MOV_PKT = 400;
    /** Set player direction. */
    public static final int DIR_PKT = 401;
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
     * A packet describing a player death.
     */
    public static class DiePacket extends Packet {
        
        /** The ID of the player that died. */
        private int player;
        
        /**
         * Creates a die packet from a player ID.
         * 
         * @param player The ID of the player that died.
         */
        public DiePacket(int player) {
            this.player = player;
            this.data = Integer.toString(player);
        }
        
        /**
         * Creates a die packet from a string.
         * 
         * @param data A string containing the ID of the player that died.
         */
        public DiePacket(String data) {
            this.player = Integer.parseInt(data);
            this.data = data;
        }

        /** {@inheritDoc} */
        @Override
        public int getPacketType() {
            return DIE_PKT;
        }
    }
}
