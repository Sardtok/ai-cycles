import sys
import socket
import select
import re
import random
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
            print errmsg
            sys.exit(2)

    def receive(self):
        packets = []
        done = 0
        while done == 0:
            buff = self.sock.recv(1024)
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

connection = Connection()

def turn_left():
    index = directions.index(cycles[myid - 1].direction)
    index = (index + 3) % 4
    connection.send(Packet(DIR_PKT, directions[index]))

def turn_right():
    index = directions.index(cycles[myid - 1].direction)
    index = (index + 1) % 4
    connection.send(Packet(DIR_PKT, directions[index]))

def move_cycle(player, direction):
    cycle = cycles[player - 1]
    cycle.direction = direction
    if direction == 'N':
        cycle.y = cycle.y - 1
    elif direction == 'S':
        cycle.y = cycle.y + 1
    elif direction == 'E':
        cycle.x = cycle.x + 1
    elif direction == 'W':
        cycle.x = cycle.x - 1
    grid[cycle.x][cycle.y] = player

if (len(sys.argv) > 1):
    connection.host = sys.argv[1]

connection.connect(name='joe')
packets = connection.receive()

for packet in packets:
    if packet.pkt_type == PID_PKT:
        myid = packet.int_value

    elif packet.pkt_type == MAP_PKT:
        cycle_count = packet.players
        cycle = 0
        width = packet.width
        height = packet.height
        grid = [[0] * width] * height
        grid.insert(0, [-1] * width)
        grid.append([-1] * width)
        for row in grid:
            row.insert(0, -1)
            row.append(-1)

        while cycle < cycle_count:
            cycles.append(Cycle())
            cycle = cycle + 1

    elif packet.pkt_type == POS_PKT:
        cycles[packet.player - 1].x = packet.x + 1
        cycles[packet.player - 1].y = packet.y + 1
        grid[packet.x][packet.y] = packet.player

    elif packet.pkt_type == RND_PKT:
        random.seed(packet.int_value)

#######################################
# This is where the thinking happens. #
#######################################
while cycles[myid - 1].alive:
    choice = random.random()
    direction = cycles[myid - 1].direction
    x = cycles[myid - 1].x
    y = cycles[myid - 1].y
    if direction == 'N':
        forward = grid[x][y - 1] == 0
        left = grid[x - 1][y] == 0;
        right = grid[x + 1][y] == 0;
    elif direction == 'S':
        forward = grid[x][y + 1] == 0
        left = grid[x + 1][y] == 0;
        right = grid[x - 1][y] == 0;
    elif direction == 'E':
        forward = grid[x + 1][y] == 0
        left = grid[x][y - 1] == 0;
        right = grid[x][y + 1] == 0;
    elif direction == 'W':
        forward = grid[x - 1][y] == 0
        left = grid[x][y + 1] == 0;
        right = grid[x][y - 1] == 0;

    if choice < 0.3 and left:
        turn_left()
    elif choice > 0.7 and right:
        turn_right()
    elif not forward:
        if right:
            turn_right()
        else:
            turn_left()

#######################################
# Handle updates from the server.     #
# receive will block until there's an #
# update packet.                      #
####################################### 
    packets = connection.receive()
    for packet in packets:
        if packet.pkt_type == MOV_PKT:
            move_cycle(packet.player, packet.direction)
        elif packet.pkt_type == DIE_PKT:
            cycles[packet.int_value - 1].alive = False
        elif packet.pkt_type == BYE_PKT:
            cycles[myid - 1].alive = False

connection.send(Packet(BYE_PKT, 'So long, suckers!'))
connection.close()
