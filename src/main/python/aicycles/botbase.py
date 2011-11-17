import sys
import socket
import select
import re
import random
import threading
import numpy as np
from packets import *
from gamegrid import *

class Connection:
    old = ''
    regexp = re.compile('^(?P<type>[0-9]{3}) (?P<data>[^\r\n]*)[\r\n]+', re.MULTILINE)
    sock = None
    host = 'localhost'
    port = '1982'

    def connect(self, name='joe'):
        try:
            self.sock = socket.create_connection((self.host, self.port))
        except socket.error as (errno, errmsg):
            print "Could not connect to game grid:"
            print errmsg
            sys.exit(1)
        packets = self.receive()
        self.send(packet=Packet(SHK_PKT, name))

    def send(self, packet):
        total = 0
        data = str(packet)
        try:
            while total < len(data):
                sent = self.sock.send(data[total:])
                total = total + sent
                if sent == 0:
                    raise RuntimeException('Master Control Program refuses to communicate')
        except socket.error as (errno, errmsg):
            print 'Master Control Program refuses to communicate'
            print errmsg, errno
            sys.exit(2)

    def receive(self):
        packets = []
        done = 0
        while done == 0:
            try:
                buff = self.sock.recv(1024)
            except socket.error as (errno, errmsg):
                print "Socket error!"
                print errmsg, errno
                sys.exit(2)
            if buff == '':
                print 'Master Control Program is silent!'
                return packets
            
            self.old = self.old + buff

            while done == 0 and len(self.old) > 4:
                match = self.regexp.search(self.old)
                if match is None:
                    break
                pkt_type = int(match.group('type'))
                data = match.group('data')
                self.old = self.old[4 + len(data):]
                
                self.old = self.old.lstrip()

                if (pkt_type == PID_PKT
                    or pkt_type == RND_PKT
                    or pkt_type == UPD_PKT
                    or pkt_type == DIE_PKT):
                    packets.append(IntPacket(pkt_type, data=data))
                elif pkt_type == MAP_PKT:
                    packets.append(MapPacket(data=data))
                elif pkt_type == MOV_PKT:
                    packets.append(MovePacket(data=data))
                elif pkt_type == POS_PKT:
                    packets.append(PositionPacket(pkt_type, data=data))
                else:
                    packets.append(Packet(pkt_type, data))

                if (pkt_type == SHK_PKT
                    or pkt_type == UPD_PKT
                    or pkt_type == BYE_PKT):
                    done = 1
        return packets
                    
    def close(self):
        self.sock.shutdown(socket.SHUT_RDWR)
        self.sock.close()

class BotBase(threading.Thread):
    def __init__(self, name="joe", bye_msg="So long, suckers!", host="localhost"):
        threading.Thread.__init__(self)
        self.connection = Connection()
        self.name = name
        self.connection.host = host
        self.running = False

        self.cycle_count = 0
        self.myid = 0

        self.cycles = []
        self.grid = []
        self.bye_msg = bye_msg

    def north(self):
        self.connection.send(Packet(DIR_PKT, "N"))
    def south(self):
        self.connection.send(Packet(DIR_PKT, "S"))
    def west(self):
        self.connection.send(Packet(DIR_PKT, "W"))
    def east(self):
        self.connection.send(Packet(DIR_PKT, "E"))
    def move(self, d):
        self.connection.send(Packet(DIR_PKT, d))

    def turn_left(self):
        index = directions.index(self.cycles[self.myid - 1].direction)
        index = (index + 3) % 4
        self.connection.send(Packet(DIR_PKT, directions[index]))

    def turn_right(self):
        index = directions.index(self.cycles[self.myid - 1].direction)
        index = (index + 1) % 4
        self.connection.send(Packet(DIR_PKT, directions[index]))

    def move_cycle(self, player, direction):
        cycle = self.cycles[player - 1]
        cycle.direction = direction
        if direction == 'N':
            cycle.y = cycle.y - 1
        elif direction == 'S':
            cycle.y = cycle.y + 1
        elif direction == 'E':
            cycle.x = cycle.x + 1
        elif direction == 'W':
            cycle.x = cycle.x - 1
        self.grid[cycle.x][cycle.y] = player

    def run(self):
        """
        Every implementation of run should look like this:

        self.start_listen()
        while self.cycles[self.myid - 1].alive:
            # Think
            ...
            self.update()
        self.end()
        """
        raise NotImplementedError

    def start_listen(self):
        self.connection.connect(name=self.name)
        packets = self.connection.receive()

        for packet in packets:
            if packet.pkt_type == PID_PKT:
                self.myid = packet.int_value

            elif packet.pkt_type == MAP_PKT:
                self.cycle_count = packet.players
                cycle = 0
                self.width = packet.width
                self.height = packet.height
                #print "Map =", width, height
                self.grid = np.zeros((self.width + 2, self.height + 2))
                
                # Make a border
                self.grid[:,0] = -1
                self.grid[:,self.width+1] = -1
                self.grid[self.height+1,:] = -1
                self.grid[0,:] = -1
                
                #print dump_grid(self.grid)

                while cycle < self.cycle_count:
                    self.cycles.append(Cycle())
                    cycle = cycle + 1

            elif packet.pkt_type == POS_PKT:
                self.cycles[packet.player - 1].x = packet.x + 1
                self.cycles[packet.player - 1].y = packet.y + 1
                self.grid[packet.x + 1][packet.y + 1] = packet.player

            elif packet.pkt_type == RND_PKT:
                random.seed(packet.int_value)
            else:
                print "We are in trouble:", packet
                
    def update(self):
        """
        Handle updates from the server.
        receive will block until there's an update packet.
        """
        packets = self.connection.receive()
        for packet in packets:
            #print packet
            if packet.pkt_type == MOV_PKT:
                self.move_cycle(packet.player, packet.direction)
            elif packet.pkt_type == DIE_PKT:
                self.cycles[packet.int_value - 1].alive = False
            elif packet.pkt_type == BYE_PKT:
                self.cycles[self.myid - 1].alive = False

    def end(self):
        try:
            self.connection.send(Packet(BYE_PKT, self.bye_msg))
            self.connection.close()
        except Exception, e:
            print "Err", e, dir(e), e.errno, e.__class__

class AwesomeBot(BotBase):
    def run(self):
        self.start_listen()
        while self.cycles[self.myid - 1].alive:
            choice = random.random()
            direction = self.cycles[self.myid - 1].direction
            x = self.cycles[self.myid - 1].x
            y = self.cycles[self.myid - 1].y
            if direction == 'N':
                forward = self.grid[x][y - 1] == 0
                left = self.grid[x - 1][y] == 0;
                right = self.grid[x + 1][y] == 0;
            elif direction == 'S':
                forward = self.grid[x][y + 1] == 0
                left = self.grid[x + 1][y] == 0;
                right = self.grid[x - 1][y] == 0;
            elif direction == 'E':
                forward = self.grid[x + 1][y] == 0
                left = self.grid[x][y - 1] == 0;
                right = self.grid[x][y + 1] == 0;
            elif direction == 'W':
                forward = self.grid[x - 1][y] == 0
                left = self.grid[x][y + 1] == 0;
                right = self.grid[x][y - 1] == 0;

            if choice < 0.3 and left:
                self.turn_left()
            elif choice > 0.7 and right:
                self.turn_right()
            elif not forward:
                if right:
                    self.turn_right()
                else:
                    self.turn_left()
            self.update()

        self.end()

def _main():
    if (len(sys.argv) > 1):
        b = AwesomeBot(host=sys.argv[1])
    else:
        b = AwesomeBot()
    b.start()

if __name__ == "__main__": _main()
