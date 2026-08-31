def cubic_bezier(p0, p1, p2, p3, t):
    x = (1-t)**3 * p0[0] + 3*(1-t)**2*t * p1[0] + 3*(1-t)*t**2 * p2[0] + t**3 * p3[0]
    y = (1-t)**3 * p0[1] + 3*(1-t)**2*t * p1[1] + 3*(1-t)*t**2 * p2[1] + t**3 * p3[1]
    return (x, y)

cx, cy = 0, 0
s = 1

pts = [
    # Up main stem, curve left into top loop
    [(0, 3), (-2, 0), (-3, -6), (-1, -8)],
    # Top loop rounding over right and down
    [(-1, -8), (2, -10), (4, -6), (1, -2)],
    # Cross center going down-left
    [(1, -2), (-2, 2), (-6, 6), (-8, 4)],
    # Bottom loop rounding under right and up
    [(-8, 4), (-10, 1), (-6, -1), (-2, 1)],
    # Cross center going right
    [(-2, 1), (2, 3), (8, 4), (10, 2)],
    # Right loop round over and back left
    [(10, 2), (12, -1), (8, -2), (4, 1)],
    # Back to center
    [(4, 1), (2, 2), (1, 3), (0, 3)]
]

grid = [[' ' for _ in range(60)] for _ in range(60)]

for segment in pts:
    for i in range(50):
        t = i / 50.0
        x, y = cubic_bezier(segment[0], segment[1], segment[2], segment[3], t)
        
        gx = int(x * 2 + 30)
        gy = int(y * 2 + 30)
        if 0 <= gx < 60 and 0 <= gy < 60:
            grid[gy][gx] = '*'

for row in grid:
    print("".join(row))
