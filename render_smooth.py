def cubic_bezier(p0, p1, p2, p3, t):
    x = (1-t)**3 * p0[0] + 3*(1-t)**2*t * p1[0] + 3*(1-t)*t**2 * p2[0] + t**3 * p3[0]
    y = (1-t)**3 * p0[1] + 3*(1-t)**2*t * p1[1] + 3*(1-t)*t**2 * p2[1] + t**3 * p3[1]
    return (x, y)

A = [
    (0, 3),      # A0 base
    (-0.5, -9),  # A1 top
    (0.5, -1),   # A2 center
    (-7, 4),     # A3 bottom left
    (-0.5, 2.5), # A4 center 2
    (8, 0),      # A5 right
    (0, 3)       # A6 = A0
]

V = [
    (-0.5, -5),  # V0 up slightly left
    (4, 0),      # V1 right
    (-4, 5),     # V2 down left
    (3, 3),      # V3 up right
    (5, -1),     # V4 right up
    (-2, -5),    # V5 down left
    (-0.5, -5)   # V6 = V0
]

k_out = [1.5, 1.5, 1.0, 1.2, 1.2, 1.0]
k_in =  [0.0, 1.5, 1.0, 1.2, 1.0, 1.5, 1.0] # index 1 to 6

pts = []
for i in range(6):
    P0 = A[i]
    P1 = (A[i][0] + V[i][0] * k_out[i], A[i][1] + V[i][1] * k_out[i])
    P2 = (A[i+1][0] - V[i+1][0] * k_in[i+1], A[i+1][1] - V[i+1][1] * k_in[i+1])
    P3 = A[i+1]
    pts.append([P0, P1, P2, P3])

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
    
# Also print the control points for Kotlin
print("KOTLIN PATH:")
for i, s in enumerate(pts):
    print(f"// Segment {i}")
    print(f"cubicTo(")
    print(f"    cx + ({s[1][0]}f) * s, cy + ({s[1][1]}f) * s,")
    print(f"    cx + ({s[2][0]}f) * s, cy + ({s[2][1]}f) * s,")
    print(f"    cx + ({s[3][0]}f) * s, cy + ({s[3][1]}f) * s")
    print(f")")

