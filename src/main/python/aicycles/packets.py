import re

SHK_PKT = 100
PID_PKT = 101
MAP_PKT = 102
POS_PKT = 103
RND_PKT = 104
BYE_PKT = 199

MOV_PKT = 400
DIR_PKT = 401
UPD_PKT = 402
DIE_PKT = 404

class Packet:
    pkt_type = 0
    data = ''
    regex = re.compile('.*^', re.MULTILINE)

    def __init__(self, pkt_type, data):
        self.pkt_type = pkt_type
        self.data = data

    def __str__(self):
        return '%d %s\n' % (self.pkt_type, self.data)

class IntPacket(Packet):
    int_value = 0
    regex = re.compile('(?P<int_value>[0-9]+)$', re.MULTILINE)
    
    def __init__(self, pkt_type, *args, **kwargs):
        if 'data' in kwargs:
            kwargs = self.regex.match(kwargs['data']).groupdict()

        self.pkt_type = pkt_type
        self.int_value = int(kwargs['int_value'])
        self.data = str(self.int_value)
        

class MapPacket(Packet):
    players = 0
    width = 0
    height = 0
    regex = re.compile('(?P<width>[0-9]+) (?P<height>[0-9]+) (?P<players>[0-9]+)$', re.MULTILINE)
    
    def __init__(self, *args, **kwargs):
        if 'data' in kwargs:
            kwargs = self.regex.match(kwargs['data']).groupdict()
            
        self.players = int(kwargs['players'])
        self.width = int(kwargs['width'])
        self.height = int(kwargs['height'])
        self.pkt_type = MAP_PKT
        self.data = "%d %d %d" % (self.width, self.height, self.players)

class PositionPacket(Packet):
    player = 0
    x = 0
    y = 0
    regex = re.compile('(?P<player>[0-9]+) (?P<x>[0-9]+) (?P<y>[0-9]+)$', re.MULTILINE)
    
    def __init__(self, *args, **kwargs):
        if 'data' in kwargs:
            kwargs = self.regex.match(kwargs['data']).groupdict()
            
        self.player = int(kwargs['player'])
        self.x = int(kwargs['x'])
        self.y = int(kwargs['y'])
        self.pkt_type = POS_PKT
        self.data = "%d %d %d" % (self.player, self.x, self.y)

class MovePacket(Packet):
    player = 0
    direction = ''
    regex = re.compile('(?P<player>[0-9]+) (?P<direction>[NESW])$', re.MULTILINE)

    def __init__(self, *args, **kwargs):
        if 'data' in kwargs:
            kwargs = self.regex.match(kwargs['data']).groupdict()
            
        self.player = int(kwargs['player'])
        self.direction = kwargs['direction']
        self.pkt_type = MOV_PKT
        self.data = "%d %s" % (self.player, self.direction)
