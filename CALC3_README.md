# Calc3 Exam 1 — TI-Nspire CX II step-by-step solver

A single-file Lua app for the TI-Nspire CX II (non-CAS) that solves the 17
question types from Calculus III Exam 1 and shows the full work line by line,
formula to substitution to simplified result, the way it is written on paper.

Files:
- `calc3.lua` — the app (math + UI in one file).
- `test_calc3.lua` — desktop test harness (Lua 5.1 compatible).

## Question types (main menu)

1. Sphere center & radius from a general equation
2. Describe 3D regions (rectangular / cylindrical / spherical) — reference
3. Meaningful vector expression? — reference
4. True / False review (15 statements) — reference
5. Vector operations: |a|, unit vector, dot, cross, angle, scalar & vector
   projection, scalar triple product / parallelepiped volume
6. Parametric & symmetric line through P and Q
7. Plane through 3 points
8. Two planes: parallel / perpendicular / angle
9. Point-to-plane distance
10. Domain & range of common multivariable functions — reference
11. Quadric surfaces and their traces — reference
12. Coordinate conversions (rect/cyl/sph, all six directions)
13. Equation → graph (surface type) — reference
14. Helix: speed, unit tangent, tangent line, arc length
15. Particle motion: velocity and position from acceleration (polynomial)
16. Projectile motion: max height, time, flight, range
17. Formula sheet — reference

## Input conventions

Solver fields accept, typed from the keypad:
- integers and decimals, including negatives: `-3`, `2.5`
- fractions: `1/2`, `-3/4`
- square roots: `sqrt(2)` (or the √ key)
- pi: `pi` (or the π key), and combinations like `2*pi/3`

Multi-value fields (particle motion `v(0)`, `r(0)`, and the acceleration
polynomials) take a comma list, constant term first:
`a_x poly = 0,2` means `a_x(t) = 0 + 2t`.

Coordinate conversion: set the `mode` field to one of
`r2c r2s c2r c2s s2r s2c`, then enter the three source coordinates in the
x/y/z fields (angles in radians).

## Navigation

- Menu: ↑/↓ to move, `enter` to open, `esc` from a screen goes back.
- Input screen: ↑/↓ or `tab` between fields, type to edit, `del` backspaces,
  `enter` solves.
- Solution screen: ↑/↓ scrolls, `esc` returns to the input (or menu).

## Output

Exact forms are shown where they are clean (`√37`, `2√2`, `4/9`, `2π/3`, `π√17`)
together with a 4-decimal value. Angles show radians, a π-fraction when one
matches, and degrees.

## Run the tests (desktop)

From this directory with any 5.1-compatible Lua:

```
lua5.1 test_calc3.lua      # or: luajit test_calc3.lua  /  lua test_calc3.lua
```

Exit status is 0 when every check passes, 1 otherwise.

## Build the .tns

`calc3.lua` is the source; the calculator loads a `.tns` document. Build it
with either tool.

### Option A — Luna (command line)

Luna ships with the Ndless SDK.

```
luna calc3.lua Calc3Exam1.tns
```

This wraps the script into a document whose first problem runs the Lua app.

### Option B — TI-Nspire Student / Teacher Software (or TI-Nspire CX II Connect)

1. `File → New Document`, choose **Add Calculator** (any page is fine).
2. `Insert → Script Editor` (menu `b` → *Insert* → *Script Editor*) to open
   the editor on a new script page.
3. Set the script name to `Calc3Exam1`.
4. Paste the entire contents of `calc3.lua` into the editor.
5. `Set Script` (the editor's *Set Script* / run button) to compile and embed it.
6. `File → Save Document As…` and save as `Calc3Exam1.tns`.

## Install on the calculator

1. Connect the TI-Nspire CX II by USB.
2. Open TI-Nspire Student Software (or TI-Nspire CX II Connect in a browser).
3. Drag `Calc3Exam1.tns` into **My Documents** on the handheld.
4. On the device: `doc` → *My Documents* → open `Calc3Exam1`.

The app fills the screen; press `esc` to back out, `menu`/arrows to navigate.
