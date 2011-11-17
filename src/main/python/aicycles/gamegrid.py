directions = ['N', 'E', 'S', 'W']

class Cycle:
    x = 0
    y = 0
    direction = 'N'
    alive = True

def dump_grid(grid):
    return "\n".join([" ".join(["%2i" % int(grid[x][y]) for x in xrange(len(grid))]) \
                          for y in xrange(len(grid[0]))])
