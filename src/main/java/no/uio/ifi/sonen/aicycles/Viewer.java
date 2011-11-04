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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
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
        new Color(255, 0, 0),
        new Color(0, 0, 255),
        new Color(0, 255, 0),
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
    
    /** The frame to display stuff in. */
    private JFrame frame;
    /** A buffer to draw the graphics to. */
    private Graphics2D buffer;
    /** The image which the above buffer represents. */
    private BufferedImage image;
    /** The size of a square. */
    private int squareSize;
    
    /** The display mode used prior to going into fullscreen mode. */
    private DisplayMode mode;
    /** The device that is used to set the graphics mode. */
    private GraphicsDevice device;
    private BufferStrategy strategy;
    
    public Viewer(boolean fullscreen) {
        frame = new JFrame("AI Cycles") {
            @Override
            public void dispose() {
                if (mode != null) {
                    device.setDisplayMode(mode);
                }
                super.dispose();
            }
        };
        
        image = new BufferedImage(HEIGHT, HEIGHT, BufferedImage.TYPE_INT_RGB);
        buffer = image.createGraphics();
        buffer.setBackground(Color.black);
        
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(WIDTH, HEIGHT));
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setBackground(Color.black);
        
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
                        if (m.getWidth() == WIDTH
                            && m.getHeight() == HEIGHT) {
                            device.setDisplayMode(m);
                            break;
                        }
                    }
                }
            }
        }

        frame.createBufferStrategy(2);
        strategy = frame.getBufferStrategy();
    }
    
    public void reset(final int width, final int height, Player[] players) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                buffer.clearRect(0, 0, image.getWidth(), image.getHeight());
                int wFactor = image.getWidth() / width;
                int hFactor = image.getHeight() / height;
                squareSize = wFactor < hFactor ? wFactor : hFactor;
            }
        });
    }
    
    public void draw(int x, int y, int player) {
        buffer.setColor(colors[player - 1 % colors.length]);
        buffer.fillRect(x * squareSize, y * squareSize, squareSize, squareSize);
    }
    
    public void draw() {
        SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               Graphics g = strategy.getDrawGraphics();
               g.drawImage(image, WIDTH-image.getWidth(), 0, null);
               strategy.show();
           } 
        });
    }
    
    public void close() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                frame.dispose();
            }
        });
    }
}
