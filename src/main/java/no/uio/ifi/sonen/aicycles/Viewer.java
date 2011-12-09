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

import no.uio.ifi.sonen.aicycles.server.Player;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * A game viewer for displaying what happens on a server.
 *
 * @author Sigmund Hansen <sigmund@chickensoft.com>
 */
public class Viewer {
    
    /** Pixel width of the screen. */
    public static final int WIDTH = 1280;
    /** Pixel height of the screen. */
    public static final int HEIGHT = 720;

    /** Twelve different player colors. */
    private static final Color[] colors = new Color[] {
        new Color(255, 64, 64),
        new Color(64, 64, 255),
        new Color(64, 255, 64),
        new Color(255, 255, 0),
        new Color(0, 255, 255),
        new Color(255, 0, 255),
        new Color(128, 255, 0),
        new Color(0, 128, 255),
        new Color(128, 0, 255),
        new Color(255, 128, 0),
        new Color(0, 255, 128),
        new Color(255, 0, 128)
    };
    
    /** Background color used for everything but the game area. */
    private static final Color bgColor = new Color(16, 16, 32);
    /** The font to use for drawing stats. */
    private Font font = new Font(Font.MONOSPACED, Font.BOLD, 32);
    
    /** Whether the game is running or to display the in-between games screen. */
    private boolean running = false;
    /** The players in the game. */
    private Player[] players;
    /** The players that are ready. */
    private boolean[] ready;
    /** The scoreboard for the current/previous game. */
    private List<Player> scoreboard = new ArrayList<Player>();
    
    /** The frame to display stuff in. */
    private JFrame frame;
    /** A buffer to draw the graphics to. */
    private Graphics2D buffer;
    /** The image which the above buffer represents. */
    private BufferedImage image;
    /** The size of a square. */
    private int squareSize;
    /** The offset to use when drawing cycles. */
    private int offset;
    
    /** The display mode used prior to going into fullscreen mode. */
    private DisplayMode mode;
    /** The device that is used to set the graphics mode. */
    private GraphicsDevice device;
    /** The buffer strategy used for drawing and double-buffering. */
    private BufferStrategy strategy;
    
    /**
     * Creates a viewer either in windowed mode, or in fullscreen mode.
     * 
     * @param fullscreen Whether to run in fullscreen mode.
     */
    public Viewer(boolean fullscreen) {
        frame = new JFrame("AI Cycles") {
            /**
             * @{inheritDoc}
             */
            @Override
            public void dispose() {
                if (mode != null) {
                    device.setDisplayMode(mode);
                }
                super.dispose();
            }
        };
        
        image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        buffer = image.createGraphics();
        buffer.setBackground(bgColor);
        
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(WIDTH, HEIGHT));
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.setBackground(bgColor);
        frame.setVisible(true);
        
        if (fullscreen) {
         device = GraphicsEnvironment.
                getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (device.isFullScreenSupported()) {
                device.setFullScreenWindow(frame);
                Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().createImage(""), new Point(), null);
                if (device.isDisplayChangeSupported()) {
                    mode = device.getDisplayMode();
                    DisplayMode[] modes = device.getDisplayModes();
                    for (DisplayMode m : modes) {
                        int depth = m.getBitDepth();
                        if (m.getWidth() == WIDTH && m.getHeight() == HEIGHT
                            && (depth >= 24 || depth == DisplayMode.BIT_DEPTH_MULTI)) {
                            device.setDisplayMode(m);
                            break;
                        }
                    }
                }
            }
        }

        frame.createBufferStrategy(2);
        strategy = frame.getBufferStrategy();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Graphics g = strategy.getDrawGraphics();
                g.clearRect(0, 0, WIDTH, HEIGHT);
                strategy.show();
            }
        });
    }
    
    /**
     * Announces that a player has connected and removes him from the waiting list.
     * 
     * @param p The player that has connected.
     */
    public void setReady(Player p) {
        if (players[p.getId() - 1] != p) {
            return;
        }
        
        ready[p.getId() - 1] = true;
        for (int i = 0; i < ready.length; i++) {
            if (!ready[i]) {
                draw();
                return;
            }
        }
        
        scoreboard.clear();
        running = true;
        draw();
    }
    
    /**
     * Adds a player to the list of dead players.
     * 
     * @param p The player to add to the list of dead players.
     */
    public void setDead(Player p) {
        if (players[p.getId() - 1] != p) {
            return;
        }
        
        scoreboard.add(p);
    }
    
    /**
     * Clears the window.
     * 
     * @param width The width of the game grid.
     * @param height The height of the game grid.
     * @param players The players that are participating.
     */
    public void reset(final int width, final int height, final Player[] players) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                buffer.clearRect(0, 0, image.getWidth(), image.getHeight());
                int wFactor = image.getWidth() / width;
                int hFactor = image.getHeight() / height;
                squareSize = wFactor < hFactor ? wFactor : hFactor;
                offset = image.getWidth() - squareSize * width;
                buffer.setColor(Color.BLACK);
                buffer.fillRect(offset, 0,
                                image.getWidth() - offset, squareSize * height);
                running = false;
                ready = new boolean[players.length];
                Viewer.this.players = players;
            }
        });
        draw();
    }
    
    /**
     * Fills a square in the image buffer.
     * 
     * @param x The horizontal position of the square.
     * @param y The vertical position of the square.
     * @param player The ID of the player to draw here.
     */
    public void draw(int x, int y, int player) {
        buffer.setColor(colors[player - 1 % colors.length]);
        buffer.fillRect(x * squareSize + offset, y * squareSize, squareSize, squareSize);
    }
    
    /**
     * Draws the list of players the server is waiting for.
     * 
     * @param g The graphics to draw to.
     */
    private void drawQueue(Graphics g) {
        FontMetrics fm = g.getFontMetrics(font);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString("Stats for last game:", 10, fm.getHeight());
        g.drawString("Waiting for:", (WIDTH - fm.stringWidth("Waiting for:"))/2, HEIGHT/2);
        int count = 0;
        for (int i = 0; i < players.length; i++) {
            if (!ready[i]) {
                count++;
                String name = players[i].getName();
                g.setColor(colors[i]);
                g.drawString(name,
                             (WIDTH - fm.stringWidth(name)) / 2,
                             HEIGHT / 2 + fm.getHeight() * count);
            }
        }
    }
    
    /**
     * Draws the player list and player placements/scores.
     * 
     * @param g The graphics object to draw to.
     */
    private void drawScores(Graphics g) {
        int line = 2;
        FontMetrics fm = g.getFontMetrics(font);
        g.setFont(font);
        if (running) {
            g.setColor(Color.WHITE);
            g.drawString("Players:", 10, fm.getHeight());
            for (int i = 0; i < players.length; i++) {
                if (!players[i].isAlive()) {
                    continue;
                }
                g.setColor(colors[i]);
                g.drawString(players[i].getName(), 10, fm.getHeight() * line++);
            }
        }
        
        line += scoreboard.size() - 1;
        int place = players.length;
        int decrease = 0;
        Player prev = null;
        for (Player p : scoreboard) {
            if (prev != null && prev.getUpdates() != p.getUpdates()) {
                place -= decrease;
                decrease = 0;
            }
            g.setColor(colors[p.getId() - 1]);
            g.drawString(String.format("%d %s", place, p.getName()),
                         10, fm.getHeight() * line--);
            decrease++;
            prev = p;
        }
    }
    
    /**
     * Draws the window.
     */
    public void draw() {
        SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               Graphics g = strategy.getDrawGraphics();
               g.clearRect(0, 0, WIDTH, HEIGHT);
               if (running) {
                   g.drawImage(image, WIDTH-image.getWidth(), 0, null);
               } else {
                   drawQueue(g);
               }
               drawScores(g);
               strategy.show();
           } 
        });
    }
    
    /**
     * Closes the window.
     */
    public void close() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                frame.dispose();
            }
        });
    }
}
