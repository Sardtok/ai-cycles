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

import java.net.Socket;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * A connection between a client and server.
 *
 * @author Sigmund Hansen <sigmunha@ifi.uio.no>
 */
public class Connection {

    /** The connection's socket. */
    Socket sock;
    /** The socket's input stream as a scanner. */
    Scanner in;
    /** The socket's output stream as a print stream. */
    PrintStream out;

    /**
     * Creates a connection from a socket.
     * 
     * @param sock The socket to create a connection from.
     * @throws IOException If either of the socket's streams could not be opened.
     */
    public Connection(Socket sock) throws IOException {
        this.sock = sock;
        in = new Scanner(sock.getInputStream());
        out = new PrintStream(sock.getOutputStream());
    }

    /**
     * Waits for data and creates a packet object from it.
     * 
     * @return The packet that was read.
     */
    public Packet receivePacket() throws IOException, MalformedPacketException {
        try {
            int packetType = in.nextInt();

            switch (packetType) {
                case Packet.MOV_PKT:
                    return new Packet.MovePacket(in.nextLine());
                case Packet.DIR_PKT:
                    return new Packet.DirectionPacket(in.nextLine());
                case Packet.DIE_PKT:
                    return new Packet.DiePacket(in.nextLine());
            }
            
        } catch (Exception e) {
            IOException ioe = in.ioException();
            if (ioe != null) {
                throw ioe;
            }

            throw new MalformedPacketException(e);
        }

        return null;
    }

    /**
     * Sends a packet on this connection.
     * 
     * @param p The packet to send.
     */
    public void sendPacket(Packet p) {
        out.printf("%d %s%n", p.getPacketType(), p.getData());
        out.flush();
    }
    /**
     * Checks if the socket has been closed, is disconnected
     * or if either of its streams are closed.
     * If the socket isn't connected, or one of the streams have been closed,
     * the socket is closed.
     * 
     * @return true if the socket is closed, disconnected
     *         or a one-way connection.
     */
    public boolean isDown() {
        if (sock.isClosed()) {
            return true;
            
        } else if (sock.isConnected()) {
            try {
                sock.close();
            } catch (IOException ioe) {
                System.err.println("Problem closing unconnected socket.");
                System.err.println(ioe.getMessage());
            }
            
            return true;
            
        } else if (sock.isInputShutdown() || sock.isOutputShutdown()) {
            try {
                sock.close();
            } catch (IOException ioe) {
                System.err.println("Problem closing one-way socket.");
                System.err.println(ioe.getMessage());
            }
            
            return true;
        }
        
        return false;
    }
}
