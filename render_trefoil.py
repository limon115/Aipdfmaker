import math

grid = [[' ' for _ in range(40)] for _ in range(40)]

for i in range(200):
    t = i * 2 * math.pi / 200.0
    # Trefoil knot (rotated/adjusted)
    # The PDF logo has a lobe pointing UP, a lobe pointing DOWN-LEFT, a lobe pointing RIGHT
    # x = sin(t) + 2 sin(2t)
    # y = cos(t) - 2 cos(2t)
    x = math.sin(t) + 2 * math.sin(2*t)
    y = math.cos(t) - 2 * math.cos(2*t)
    
    # Scale to fit
    gx = int(x * 6 + 20)
    gy = int(y * 6 + 20)
    if 0 <= gx < 40 and 0 <= gy < 40:
        grid[gy][gx] = '*'

for row in grid:
    print("".join(row))
