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

/**
 * A very simple (random) bot.
 *
 * @author Sigmund Hansen <sigmund@chickensoft.com>
 */
public class AwesomeBot extends BotBase implements Runnable {

    Direction[] dirs = Direction.values();
    
    public AwesomeBot(String server) {
        super(server);
    }
    
    @Override
    public void start() {
        if (running) {
            return;
        }
        
        super.start();
        new Thread(this).start();
    }
    
    public static void main(String[] args) {
        AwesomeBot pb;
        if (args.length >= 1) {
            pb = new AwesomeBot(args[0]);
        } else {
            pb = new AwesomeBot("localhost");
        }
        pb.start();
    }
    
    public void run() {
        while (cycles[id - 1].isAlive()) {
            synchronized(this) {
                try {
                    this.wait();
                    if (!cycles[id - 1].isAlive()) {
                        break;
                    }
                    
                } catch (InterruptedException e) { }
            }

            double chance = Math.random();
            Cycle c = cycles[id-1];
            Direction dir = c.getDirection();
            int x = c.getX();
            int y = c.getY();
            boolean forward = false, left = false, right = false;
            switch (dir) {
                case N:
                    forward = map[x][y - 1];
                    left = map[x - 1][y];
                    right = map[x + 1][y];
                    break;
                case E:
                    forward = map[x + 1][y ];
                    left = map[x][y - 1];
                    right = map[x][y + 1];
                    break;
                case W:
                    forward = map[x - 1][y ];
                    left = map[x][y + 1];
                    right = map[x][y - 1];
                    break;
                case S:
                    forward = map[x][y + 1];
                    left = map[x + 1][y];
                    right = map[x - 1][y];
                    break;
            }
            
            if (chance <= 0.3 && !left) {
                turnLeft();
            } else if (chance >= 0.7 && !right) {
                turnRight();
            } else if (forward) {
                if (!right) {
                    turnRight();
                } else {
                    turnLeft();
                }
            }
        }
    }
    
    @Override
    public String getName() {
        return "joe";
    }
    
}
