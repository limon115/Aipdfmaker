def cubic_bezier(p0, p1, p2, p3, t):
    x = (1-t)**3 * p0[0] + 3*(1-t)**2*t * p1[0] + 3*(1-t)*t**2 * p2[0] + t**3 * p3[0]
    y = (1-t)**3 * p0[1] + 3*(1-t)**2*t * p1[1] + 3*(1-t)*t**2 * p2[1] + t**3 * p3[1]
    return (x, y)

cx, cy = 0, 0
s = 1

pts = [
    [(0, 3), (-1, -2), (-2, -7), (-2, -10)],
    [(-2, -10), (-2, -14), (3, -14), (3, -10)],
    [(3, -10), (3, -4), (-6, 5), (-9, 5)],
    [(-9, 5), (-12, 5), (-12, 1), (-9, 1)],
    [(-9, 1), (-4, 1), (4, 1), (9, 1)],
    [(9, 1), (14, 1), (14, 7), (9, 7)],
    [(9, 7), (5, 7), (1, 5), (0, 3)]
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
