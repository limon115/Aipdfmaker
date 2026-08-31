def cubic_bezier(p0, p1, p2, p3, t):
    x = (1-t)**3 * p0[0] + 3*(1-t)**2*t * p1[0] + 3*(1-t)*t**2 * p2[0] + t**3 * p3[0]
    y = (1-t)**3 * p0[1] + 3*(1-t)**2*t * p1[1] + 3*(1-t)*t**2 * p2[1] + t**3 * p3[1]
    return (x, y)

cx, cy = 0, 0
s = 1

pts = [
    # Top loop
    [(cx, cy), (cx-2*s, cy-6*s), (cx-3*s, cy-12*s), (cx+1*s, cy-14*s)],
    [(cx+1*s, cy-14*s), (cx+5*s, cy-12*s), (cx+2*s, cy-2*s), (cx-2*s, cy+2*s)],
    # Bottom left loop
    [(cx-2*s, cy+2*s), (cx-8*s, cy+8*s), (cx-12*s, cy+6*s), (cx-8*s, cy+1*s)],
    # Cross center
    [(cx-8*s, cy+1*s), (cx-5*s, cy-3*s), (cx+2*s, cy+2*s), (cx+6*s, cy+4*s)],
    # Right loop
    [(cx+6*s, cy+4*s), (cx+12*s, cy+7*s), (cx+14*s, cy+4*s), (cx+10*s, cy+2*s)],
    # Return
    [(cx+10*s, cy+2*s), (cx+6*s, cy+0*s), (cx+2*s, cy+1*s), (cx, cy)]
]

grid = [[' ' for _ in range(40)] for _ in range(40)]

for segment in pts:
    for i in range(50):
        t = i / 50.0
        x, y = cubic_bezier(segment[0], segment[1], segment[2], segment[3], t)
        
        gx = int(x + 20)
        gy = int(y + 20)
        if 0 <= gx < 40 and 0 <= gy < 40:
            grid[gy][gx] = '*'

for row in grid:
    print("".join(row))
