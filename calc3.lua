-- Calc3 Exam 1 : TI-Nspire CX II step-by-step Calculus III solver
-- Target: TI-Nspire CX II, Lua apiLevel 2.7, non-CAS.
-- Package .lua -> .tns with Luna or TI-Nspire Student Software.
--
-- Design: pure math/solver logic (testable off-device) is kept separate from
-- the on-device UI. Every solver returns a flat list of display lines so the
-- solution screen is a simple scrollable text list. Each line shows the work
-- exactly as written on paper: formula -> substitution (negatives in
-- parentheses) -> simplified result, one step per line.

local M = {}            -- public table (solvers + helpers), used by UI and tests
local abs, sqrt, floor = math.abs, math.sqrt, math.floor
local pi = math.pi

----------------------------------------------------------------------
-- NUMBER / EXACT-FORM HELPERS
----------------------------------------------------------------------

local function isInt(x) return x == floor(x) end

local function gcd(a, b)
  a, b = abs(a), abs(b)
  while b ~= 0 do a, b = b, a % b end
  return a
end

-- 4-decimal display, cleaned of "-0.0000"
local function d4(x)
  if x ~= x then return "undef" end
  local s = string.format("%.4f", x)
  if s == "-0.0000" then s = "0.0000" end
  return s
end
M.d4 = d4

-- compact number for display (integers without trailing zeros, else 4 dp)
local function num(x)
  if type(x) ~= "number" then return tostring(x) end
  if isInt(x) then return string.format("%d", x) end
  return d4(x)
end
M.num = num

-- reduced fraction string for integer numerator/denominator
local function fracStr(n, dd)
  if dd == 0 then return "undef" end
  if dd < 0 then n, dd = -n, -dd end
  local g = gcd(n, dd)
  if g == 0 then g = 1 end
  n, dd = n / g, dd / g
  if dd == 1 then return string.format("%d", n) end
  return string.format("%d/%d", n, dd)
end
M.fracStr = fracStr

-- simplify sqrt(n) for integer n >= 0 -> coeff, radicand with coeff*sqrt(rad)
local function simplifySqrt(n)
  if n < 0 then return nil end
  if n == 0 then return 0, 1 end
  local coeff, rad = 1, n
  local i = floor(sqrt(n))
  while i >= 2 do
    local sq = i * i
    if rad % sq == 0 then coeff, rad = i, rad / sq break end
    i = i - 1
  end
  return coeff, rad
end
M.simplifySqrt = simplifySqrt

-- exact string for sqrt(n), integer n >= 0 : "5", "√37", "2√2", "0"
local function sqrtStr(n)
  if n < 0 then return "undef" end
  if not isInt(n) then return "√("..num(n)..")" end
  local c, r = simplifySqrt(n)
  if r == 1 then return string.format("%d", c) end
  if c == 1 then return "√"..string.format("%d", r) end
  return string.format("%d√%d", c, r)
end
M.sqrtStr = sqrtStr

-- recognise theta as a simple fraction of pi -> "2π/3", "π", "π/4", or nil
local function piFracStr(theta)
  if theta == 0 then return "0" end
  for q = 1, 12 do
    for p = 1, 2 * q do
      if abs(theta - p * pi / q) < 1e-9 then
        local s = fracStr(p, q)           -- reduce p/q
        if s == "1" then return "π" end
        -- rebuild as (num)π/(den)
        local num_, den_ = p, q
        local g = gcd(num_, den_); num_, den_ = num_ / g, den_ / g
        if den_ == 1 then
          if num_ == 1 then return "π" end
          return string.format("%dπ", num_)
        end
        if num_ == 1 then return string.format("π/%d", den_) end
        return string.format("%dπ/%d", num_, den_)
      end
    end
  end
  return nil
end
M.piFracStr = piFracStr

-- angle presented exactly (if a π-fraction) with degrees + radian decimal
local function angleStr(theta)
  local deg = theta * 180 / pi
  local pf = piFracStr(theta)
  local exact = pf and (pf .. " rad") or (d4(theta) .. " rad")
  return string.format("%s = %s° (%s rad)", exact, d4(deg), d4(theta))
end
M.angleStr = angleStr

-- portable atan2 (TI Lua 5.1 has math.atan2; newer Lua uses math.atan(y,x)).
-- Implemented directly so behaviour is identical on every Lua version.
local function atan2(y, x)
  if x > 0 then return math.atan(y / x) end
  if x < 0 then
    if y >= 0 then return math.atan(y / x) + pi else return math.atan(y / x) - pi end
  end
  -- x == 0
  if y > 0 then return pi / 2 end
  if y < 0 then return -pi / 2 end
  return 0
end
M.atan2 = atan2

-- quadrant-aware angle for coordinate conversions, result in [0, 2π)
local function atan2q(y, x)
  local t = atan2(y, x)
  if t < 0 then t = t + 2 * pi end
  return t
end
M.atan2q = atan2q

local function quadrantNote(x, y)
  if x == 0 and y == 0 then return "origin: θ undefined (choose 0)" end
  if x == 0 then return (y > 0) and "on +y axis" or "on -y axis" end
  if y == 0 then return (x > 0) and "on +x axis" or "on -x axis" end
  local q
  if x > 0 and y > 0 then q = "I"
  elseif x < 0 and y > 0 then q = "II"
  elseif x < 0 and y < 0 then q = "III"
  else q = "IV" end
  return "quadrant " .. q
end
M.quadrantNote = quadrantNote

-- parenthesise a number when substituting (negatives get parentheses)
local function pz(x)
  local s = num(x)
  if type(x) == "number" and x < 0 then return "(" .. s .. ")" end
  return s
end

----------------------------------------------------------------------
-- VECTOR HELPERS
----------------------------------------------------------------------

local function vstr(v) return string.format("⟨%s, %s, %s⟩", num(v[1]), num(v[2]), num(v[3])) end
M.vstr = vstr

local function dot(a, b) return a[1]*b[1] + a[2]*b[2] + a[3]*b[3] end
local function cross(a, b)
  return { a[2]*b[3] - a[3]*b[2],
           a[3]*b[1] - a[1]*b[3],
           a[1]*b[2] - a[2]*b[1] }
end
local function sub(a, b) return { a[1]-b[1], a[2]-b[2], a[3]-b[3] } end
local function mag2(a) return dot(a, a) end       -- squared magnitude (integer if ints)
M.dot, M.cross, M.sub, M.mag2 = dot, cross, sub, mag2

----------------------------------------------------------------------
-- SOLVERS
-- Each returns a list of strings (display lines).
----------------------------------------------------------------------

M.solvers = {}

-- line-builder helper
local function L() return {} end
local function add(t, s) t[#t + 1] = s end

-- (1) SPHERE from general equation
-- Input: A (coeff of x^2+y^2+z^2, default 1), D, E, F, G in
-- A(x^2+y^2+z^2) + Dx + Ey + Fz + G = 0
M.solvers.sphere = function(inp)
  local A = inp.A or 1
  local D, E, F, G = inp.D, inp.E, inp.F, inp.G
  local out = L()
  add(out, "Sphere from general equation")
  add(out, string.format("%sx²+%sy²+%sz² %s = 0 (A=%s)",
      num(A), num(A), num(A), "", num(A)))
  if A ~= 1 then
    add(out, "Divide through by A = " .. num(A) .. ":")
    D, E, F, G = D / A, E / A, F / A, G / A
  end
  add(out, string.format("x²+y²+z² + (%s)x + (%s)y + (%s)z + (%s) = 0",
      num(D), num(E), num(F), num(G)))
  add(out, "Complete the square in each variable:")
  add(out, string.format("center = (-D/2, -E/2, -F/2) = (%s, %s, %s)",
      num(-D/2), num(-E/2), num(-F/2)))
  local r2 = (D/2)^2 + (E/2)^2 + (F/2)^2 - G
  add(out, string.format("r² = (D/2)²+(E/2)²+(F/2)² - G"))
  add(out, string.format("r² = %s+%s+%s - %s = %s",
      num((D/2)^2), num((E/2)^2), num((F/2)^2), pz(G), num(r2)))
  if r2 < 0 then
    add(out, "r² < 0 : no real sphere (empty set)")
  elseif r2 == 0 then
    add(out, "r² = 0 : single point (degenerate sphere)")
  else
    local rexact = isInt(r2) and sqrtStr(r2) or d4(sqrt(r2))
    add(out, string.format("r = √%s = %s  (%s)", num(r2), rexact, d4(sqrt(r2))))
  end
  return out
end

-- (5) VECTOR OPERATIONS on a, b (c optional for triple product / volume)
M.solvers.vectors = function(inp)
  local a = { inp.a1, inp.a2, inp.a3 }
  local b = { inp.b1, inp.b2, inp.b3 }
  local hasC = inp.c1 ~= nil or inp.c2 ~= nil or inp.c3 ~= nil
  local c = { inp.c1 or 0, inp.c2 or 0, inp.c3 or 0 }
  local out = L()
  add(out, "a = " .. vstr(a) .. "   b = " .. vstr(b))
  if hasC then add(out, "c = " .. vstr(c)) end
  add(out, "")

  -- magnitude of a
  local ma2 = mag2(a)
  add(out, "|a| = √(a₁²+a₂²+a₃²)")
  add(out, string.format("    = √(%s+%s+%s) = √%s = %s",
      num(a[1]^2), num(a[2]^2), num(a[3]^2), num(ma2),
      (isInt(ma2) and sqrtStr(ma2) or d4(sqrt(ma2)))))
  add(out, "    ≈ " .. d4(sqrt(ma2)))

  -- unit vector of a
  local maE = isInt(ma2) and sqrtStr(ma2) or d4(sqrt(ma2))
  add(out, "")
  add(out, "unit û = a/|a|")
  add(out, string.format("    = ⟨%s/%s, %s/%s, %s/%s⟩",
      num(a[1]), maE, num(a[2]), maE, num(a[3]), maE))
  local ma = sqrt(ma2)
  add(out, string.format("    ≈ ⟨%s, %s, %s⟩", d4(a[1]/ma), d4(a[2]/ma), d4(a[3]/ma)))

  -- dot product
  local ab = dot(a, b)
  add(out, "")
  add(out, "a·b = a₁b₁+a₂b₂+a₃b₃")
  add(out, string.format("    = %s·%s + %s·%s + %s·%s",
      pz(a[1]), pz(b[1]), pz(a[2]), pz(b[2]), pz(a[3]), pz(b[3])))
  add(out, "    = " .. num(ab))

  -- cross product
  local cr = cross(a, b)
  add(out, "")
  add(out, "a×b = ⟨a₂b₃-a₃b₂, a₃b₁-a₁b₃, a₁b₂-a₂b₁⟩")
  add(out, string.format("    = ⟨%s·%s-%s·%s, %s·%s-%s·%s, %s·%s-%s·%s⟩",
      pz(a[2]),pz(b[3]),pz(a[3]),pz(b[2]),
      pz(a[3]),pz(b[1]),pz(a[1]),pz(b[3]),
      pz(a[1]),pz(b[2]),pz(a[2]),pz(b[1])))
  add(out, "    = " .. vstr(cr))

  -- angle between
  local mb2 = mag2(b)
  local cosT = ab / (sqrt(ma2) * sqrt(mb2))
  if cosT > 1 then cosT = 1 elseif cosT < -1 then cosT = -1 end
  local theta = math.acos(cosT)
  add(out, "")
  add(out, "angle: cosθ = (a·b)/(|a||b|)")
  add(out, string.format("    = %s / (√%s·√%s) = %s",
      pz(ab), num(ma2), num(mb2), d4(cosT)))
  add(out, "    θ = " .. angleStr(theta))

  -- scalar projection comp_a b and vector projection proj_a b
  add(out, "")
  add(out, "scalar proj  comp_a b = (a·b)/|a|")
  add(out, string.format("    = %s/%s = %s", pz(ab), maE, d4(ab / ma)))
  add(out, "vector proj  proj_a b = ((a·b)/|a|²) a")
  add(out, string.format("    = (%s/%s)·%s", pz(ab), num(ma2), vstr(a)))
  add(out, string.format("    ≈ ⟨%s, %s, %s⟩",
      d4(ab/ma2*a[1]), d4(ab/ma2*a[2]), d4(ab/ma2*a[3])))

  -- triple product / volume
  if hasC then
    local tp = dot(a, cross(b, c))
    add(out, "")
    add(out, "scalar triple product a·(b×c) = " .. num(tp))
    add(out, "volume of parallelepiped = |a·(b×c)| = " .. num(abs(tp)))
  end
  return out
end

-- (6) PARAMETRIC LINE through P and Q (direction = Q-P)
M.solvers.line = function(inp)
  local P = { inp.p1, inp.p2, inp.p3 }
  local Q = { inp.q1, inp.q2, inp.q3 }
  local d = sub(Q, P)
  local out = L()
  add(out, string.format("P(%s, %s, %s)  Q(%s, %s, %s)",
      num(P[1]),num(P[2]),num(P[3]),num(Q[1]),num(Q[2]),num(Q[3])))
  add(out, "direction v = Q - P")
  add(out, string.format("  = ⟨%s-%s, %s-%s, %s-%s⟩ = %s",
      num(Q[1]),pz(P[1]),num(Q[2]),pz(P[2]),num(Q[3]),pz(P[3]), vstr(d)))
  add(out, "")
  add(out, "Parametric equations (base point P):")
  add(out, string.format("  x = %s + (%s)t", num(P[1]), num(d[1])))
  add(out, string.format("  y = %s + (%s)t", num(P[2]), num(d[2])))
  add(out, string.format("  z = %s + (%s)t", num(P[3]), num(d[3])))
  add(out, "")
  add(out, "Symmetric equations:")
  local parts = {}
  local names = { "x", "y", "z" }
  for i = 1, 3 do
    if d[i] ~= 0 then
      parts[#parts+1] = string.format("(%s-%s)/%s", names[i], num(P[i]), num(d[i]))
    end
  end
  if #parts >= 2 then
    add(out, "  " .. table.concat(parts, " = "))
  end
  for i = 1, 3 do
    if d[i] == 0 then
      add(out, string.format("  with %s = %s (direction component 0)", names[i], num(P[i])))
    end
  end
  return out
end

-- (7) PLANE through 3 points P, Q, R
M.solvers.plane3 = function(inp)
  local P = { inp.p1, inp.p2, inp.p3 }
  local Q = { inp.q1, inp.q2, inp.q3 }
  local R = { inp.r1, inp.r2, inp.r3 }
  local out = L()
  add(out, string.format("P(%s,%s,%s) Q(%s,%s,%s) R(%s,%s,%s)",
      num(P[1]),num(P[2]),num(P[3]),num(Q[1]),num(Q[2]),num(Q[3]),
      num(R[1]),num(R[2]),num(R[3])))
  local PQ = sub(Q, P)
  local PR = sub(R, P)
  add(out, "PQ = Q-P = " .. vstr(PQ))
  add(out, "PR = R-P = " .. vstr(PR))
  local n = cross(PQ, PR)
  add(out, "normal n = PQ × PR")
  add(out, string.format("  = ⟨%s·%s-%s·%s, %s·%s-%s·%s, %s·%s-%s·%s⟩",
      pz(PQ[2]),pz(PR[3]),pz(PQ[3]),pz(PR[2]),
      pz(PQ[3]),pz(PR[1]),pz(PQ[1]),pz(PR[3]),
      pz(PQ[1]),pz(PR[2]),pz(PQ[2]),pz(PR[1])))
  add(out, "  = " .. vstr(n))
  -- simplify normal by gcd
  local g = gcd(gcd(abs(n[1]), abs(n[2])), abs(n[3]))
  if g > 1 then
    add(out, string.format("divide n by %s: ⟨%s, %s, %s⟩", num(g), num(n[1]/g), num(n[2]/g), num(n[3]/g)))
    n = { n[1]/g, n[2]/g, n[3]/g }
  end
  local dconst = dot(n, P)
  add(out, "")
  add(out, "Plane: n·(r - P) = 0  ->  a x + b y + c z = n·P")
  add(out, string.format("  %s·P = %s·%s + %s·%s + %s·%s = %s",
      "n", pz(n[1]),pz(P[1]),pz(n[2]),pz(P[2]),pz(n[3]),pz(P[3]), num(dconst)))
  add(out, string.format("  %sx + %sy + %sz = %s", num(n[1]), num(n[2]), num(n[3]), num(dconst)))
  return out
end

-- (8) TWO PLANES : parallel / perpendicular / angle via normals n1, n2
M.solvers.planes2 = function(inp)
  local n1 = { inp.a1, inp.b1, inp.c1 }
  local n2 = { inp.a2, inp.b2, inp.c2 }
  local out = L()
  add(out, "n₁ = " .. vstr(n1) .. "   n₂ = " .. vstr(n2))
  local d = dot(n1, n2)
  local cr = cross(n1, n2)
  add(out, "n₁·n₂ = " .. num(d))
  add(out, "n₁×n₂ = " .. vstr(cr))
  local parallel = (cr[1] == 0 and cr[2] == 0 and cr[3] == 0)
  if parallel then
    add(out, "n₁×n₂ = 0  ->  normals parallel  ->  PLANES PARALLEL")
    return out
  end
  if d == 0 then
    add(out, "n₁·n₂ = 0  ->  PLANES PERPENDICULAR (θ = 90°)")
    return out
  end
  local m1, m2 = sqrt(mag2(n1)), sqrt(mag2(n2))
  local cosT = abs(d) / (m1 * m2)
  if cosT > 1 then cosT = 1 end
  local theta = math.acos(cosT)
  add(out, "cosθ = |n₁·n₂|/(|n₁||n₂|)")
  add(out, string.format("  = %s/(√%s·√%s) = %s", num(abs(d)), num(mag2(n1)), num(mag2(n2)), d4(cosT)))
  add(out, "θ = " .. angleStr(theta))
  return out
end

-- (9) POINT-TO-PLANE distance. Plane a x+b y+c z = d (RHS form). Point (x0,y0,z0)
M.solvers.ptplane = function(inp)
  local a, b, c, d = inp.a, inp.b, inp.c, inp.d
  local x0, y0, z0 = inp.x0, inp.y0, inp.z0
  local out = L()
  add(out, string.format("Plane: %sx + %sy + %sz = %s", num(a), num(b), num(c), num(d)))
  add(out, string.format("Point: (%s, %s, %s)", num(x0), num(y0), num(z0)))
  add(out, "D = |a·x₀+b·y₀+c·z₀ - d| / √(a²+b²+c²)")
  local top = a*x0 + b*y0 + c*z0 - d
  local den2 = a*a + b*b + c*c
  add(out, string.format("  num = |%s·%s + %s·%s + %s·%s - %s| = |%s| = %s",
      pz(a),pz(x0),pz(b),pz(y0),pz(c),pz(z0),pz(d), num(top), num(abs(top))))
  add(out, string.format("  den = √(%s+%s+%s) = √%s = %s",
      num(a*a), num(b*b), num(c*c), num(den2),
      (isInt(den2) and sqrtStr(den2) or d4(sqrt(den2)))))
  local dist = abs(top) / sqrt(den2)
  add(out, string.format("  D = %s/%s = %s  (≈ %s)",
      num(abs(top)), (isInt(den2) and sqrtStr(den2) or d4(sqrt(den2))),
      (isInt(abs(top)) and isInt(den2) and (abs(top).."/"..sqrtStr(den2)) or d4(dist)), d4(dist)))
  return out
end

-- (12) COORDINATE CONVERSIONS
-- mode: "r2c" rect->cyl, "r2s" rect->sph, "c2r", "s2r", "c2s", "s2c"
M.solvers.convert = function(inp)
  local mode = inp.mode or "r2c"
  local out = L()
  if mode == "r2c" then
    local x, y, z = inp.x, inp.y, inp.z
    add(out, string.format("Rectangular (%s, %s, %s) -> Cylindrical", num(x),num(y),num(z)))
    local r2 = x*x + y*y
    add(out, string.format("r = √(x²+y²) = √(%s+%s) = √%s = %s",
        num(x*x), num(y*y), num(r2), (isInt(r2) and sqrtStr(r2) or d4(sqrt(r2)))))
    local th = atan2q(y, x)
    add(out, "θ = atan2(y, x)   (" .. quadrantNote(x, y) .. ")")
    local pf = piFracStr(th)
    add(out, string.format("  θ = %s = %s rad = %s°",
        pf or d4(th), d4(th), d4(th*180/pi)))
    add(out, "z = " .. num(z))
    add(out, string.format("=> (r, θ, z) = (%s, %s, %s)",
        (isInt(r2) and sqrtStr(r2) or d4(sqrt(r2))), (pf or d4(th)), num(z)))
  elseif mode == "c2r" then
    local r, th, z = inp.r, inp.theta, inp.z
    add(out, string.format("Cylindrical (r=%s, θ=%s, z=%s) -> Rectangular", num(r), d4(th), num(z)))
    add(out, string.format("x = r·cosθ = %s·cos(%s) = %s", num(r), d4(th), d4(r*math.cos(th))))
    add(out, string.format("y = r·sinθ = %s·sin(%s) = %s", num(r), d4(th), d4(r*math.sin(th))))
    add(out, "z = " .. num(z))
  elseif mode == "r2s" then
    local x, y, z = inp.x, inp.y, inp.z
    add(out, string.format("Rectangular (%s, %s, %s) -> Spherical", num(x),num(y),num(z)))
    local rho2 = x*x + y*y + z*z
    local rho = sqrt(rho2)
    add(out, string.format("ρ = √(x²+y²+z²) = √%s = %s",
        num(rho2), (isInt(rho2) and sqrtStr(rho2) or d4(rho))))
    local th = atan2q(y, x)
    add(out, "θ = atan2(y, x)   (" .. quadrantNote(x, y) .. ")")
    add(out, string.format("  θ = %s rad = %s°", d4(th), d4(th*180/pi)))
    local phi = (rho == 0) and 0 or math.acos(z / rho)
    add(out, string.format("φ = acos(z/ρ) = acos(%s/%s) = %s rad = %s°",
        num(z), (isInt(rho2) and sqrtStr(rho2) or d4(rho)), d4(phi), d4(phi*180/pi)))
    add(out, string.format("=> (ρ, θ, φ) = (%s, %s, %s)",
        (isInt(rho2) and sqrtStr(rho2) or d4(rho)), d4(th), d4(phi)))
  elseif mode == "s2r" then
    local rho, th, phi = inp.rho, inp.theta, inp.phi
    add(out, string.format("Spherical (ρ=%s, θ=%s, φ=%s) -> Rectangular", num(rho), d4(th), d4(phi)))
    add(out, string.format("x = ρ·sinφ·cosθ = %s", d4(rho*math.sin(phi)*math.cos(th))))
    add(out, string.format("y = ρ·sinφ·sinθ = %s", d4(rho*math.sin(phi)*math.sin(th))))
    add(out, string.format("z = ρ·cosφ = %s", d4(rho*math.cos(phi))))
  elseif mode == "c2s" then
    local r, th, z = inp.r, inp.theta, inp.z
    add(out, string.format("Cylindrical (r=%s, θ=%s, z=%s) -> Spherical", num(r), d4(th), num(z)))
    local rho2 = r*r + z*z
    local rho = sqrt(rho2)
    add(out, string.format("ρ = √(r²+z²) = √%s = %s", num(rho2), (isInt(rho2) and sqrtStr(rho2) or d4(rho))))
    add(out, "θ unchanged = " .. d4(th))
    local phi = (rho == 0) and 0 or math.acos(z / rho)
    add(out, string.format("φ = acos(z/ρ) = %s rad = %s°", d4(phi), d4(phi*180/pi)))
  elseif mode == "s2c" then
    local rho, th, phi = inp.rho, inp.theta, inp.phi
    add(out, string.format("Spherical (ρ=%s, θ=%s, φ=%s) -> Cylindrical", num(rho), d4(th), d4(phi)))
    add(out, string.format("r = ρ·sinφ = %s", d4(rho*math.sin(phi))))
    add(out, "θ unchanged = " .. d4(th))
    add(out, string.format("z = ρ·cosφ = %s", d4(rho*math.cos(phi))))
  end
  return out
end

-- (14) HELIX r(t)=⟨a·cos(ω t), a·sin(ω t), c·t⟩ properties
-- inputs: a, w (omega), c, t0, t1 (arc length over [t0,t1]), tangent at t0
M.solvers.helix = function(inp)
  local a, w, c = inp.a, inp.w, inp.c
  local t0, t1 = inp.t0 or 0, inp.t1
  local out = L()
  add(out, string.format("r(t) = ⟨%s·cos(%st), %s·sin(%st), %s·t⟩",
      num(a), num(w), num(a), num(w), num(c)))
  add(out, string.format("r'(t) = ⟨-%s%s·sin(%st), %s%s·cos(%st), %s⟩",
      num(a), (w==1 and "" or ("·"..num(w))), num(w),
      num(a), (w==1 and "" or ("·"..num(w))), num(w), num(c)))
  -- |r'(t)| = sqrt(a^2 w^2 + c^2), constant
  local sp2 = a*a*w*w + c*c
  add(out, "|r'(t)| = √(a²ω²·(sin²+cos²) + c²) = √(a²ω² + c²)")
  add(out, string.format("  = √(%s·%s + %s) = √%s = %s",
      num(a*a), num(w*w), num(c*c), num(sp2),
      (isInt(sp2) and sqrtStr(sp2) or d4(sqrt(sp2)))))
  add(out, "  ≈ " .. d4(sqrt(sp2)) .. "  (constant speed)")
  local sp = sqrt(sp2)
  -- unit tangent at t0
  add(out, "")
  add(out, "Unit tangent T(t) = r'(t)/|r'(t)|")
  local rp0 = { -a*w*math.sin(w*t0), a*w*math.cos(w*t0), c }
  add(out, string.format("T(%s) ≈ ⟨%s, %s, %s⟩",
      num(t0), d4(rp0[1]/sp), d4(rp0[2]/sp), d4(rp0[3]/sp)))
  -- tangent line at t0
  local r0 = { a*math.cos(w*t0), a*math.sin(w*t0), c*t0 }
  add(out, "")
  add(out, string.format("Point r(%s) ≈ (%s, %s, %s)", num(t0), d4(r0[1]), d4(r0[2]), d4(r0[3])))
  add(out, "Tangent line: R(s) = r(t₀) + s·r'(t₀)")
  add(out, string.format("  x = %s + (%s)s", d4(r0[1]), d4(rp0[1])))
  add(out, string.format("  y = %s + (%s)s", d4(r0[2]), d4(rp0[2])))
  add(out, string.format("  z = %s + (%s)s", d4(r0[3]), d4(rp0[3])))
  -- arc length
  if t1 ~= nil then
    add(out, "")
    add(out, "Arc length L = ∫|r'(t)|dt from t₀ to t₁ = |r'|·(t₁-t₀)")
    local Lv = sp * (t1 - t0)
    add(out, string.format("  = %s·(%s-%s) = %s  (≈ %s)",
        (isInt(sp2) and sqrtStr(sp2) or d4(sp)), num(t1), num(t0),
        (isInt(sp2) and (num(t1-t0).."·"..sqrtStr(sp2)) or d4(Lv)), d4(Lv)))
  end
  return out
end

-- (15) PARTICLE MOTION : v(t) from a(t), position from v(t)
-- acceleration components are polynomials given as coefficient lists
-- (constant term first). initial velocity v0, initial position r0.
local function polyIntegrate(coefs)        -- returns antiderivative coefs with 0 constant
  local r = { 0 }
  for i = 1, #coefs do r[i + 1] = coefs[i] / i end
  return r
end
local function polyStr(coefs, var)
  var = var or "t"
  local terms = {}
  for i = #coefs, 1, -1 do
    local ccoef = coefs[i]
    if ccoef ~= 0 then
      local pw = i - 1
      local cs = num(ccoef)
      if pw == 0 then terms[#terms+1] = cs
      elseif pw == 1 then terms[#terms+1] = cs .. var
      else terms[#terms+1] = cs .. var .. "^" .. pw end
    end
  end
  if #terms == 0 then return "0" end
  return table.concat(terms, " + ")
end
local function polyEval(coefs, t)
  local s, p = 0, 1
  for i = 1, #coefs do s = s + coefs[i] * p; p = p * t end
  return s
end

M.solvers.particle = function(inp)
  -- inp.ax, inp.ay, inp.az : coefficient lists (constant first)
  -- inp.v0 : {vx,vy,vz}   inp.r0 : {x,y,z}  inp.teval optional
  local out = L()
  local comps = { { "x", inp.ax }, { "y", inp.ay }, { "z", inp.az } }
  add(out, "Acceleration a(t):")
  for _, cc in ipairs(comps) do
    add(out, string.format("  a_%s = %s", cc[1], polyStr(cc[2] or {0})))
  end
  add(out, "Integrate a(t) to get v(t), use v(0) for +C:")
  local vcoefs = {}
  for i, cc in ipairs(comps) do
    local anti = polyIntegrate(cc[2] or {0})
    anti[1] = (inp.v0 and inp.v0[i]) or 0        -- constant = v0 component
    vcoefs[i] = anti
    add(out, string.format("  v_%s = %s", cc[1], polyStr(anti)))
  end
  add(out, "Integrate v(t) to get r(t), use r(0) for +C:")
  local rcoefs = {}
  for i, cc in ipairs(comps) do
    local anti = polyIntegrate(vcoefs[i])
    anti[1] = (inp.r0 and inp.r0[i]) or 0
    rcoefs[i] = anti
    add(out, string.format("  %s = %s", cc[1], polyStr(anti)))
  end
  if inp.teval ~= nil then
    local t = inp.teval
    add(out, "")
    add(out, string.format("At t = %s:", num(t)))
    add(out, string.format("  v = ⟨%s, %s, %s⟩",
        d4(polyEval(vcoefs[1],t)), d4(polyEval(vcoefs[2],t)), d4(polyEval(vcoefs[3],t))))
    add(out, string.format("  r = ⟨%s, %s, %s⟩",
        d4(polyEval(rcoefs[1],t)), d4(polyEval(rcoefs[2],t)), d4(polyEval(rcoefs[3],t))))
  end
  return out
end

-- (16) PROJECTILE MOTION : launch angle alpha (deg), speed v0, gravity g
M.solvers.projectile = function(inp)
  local alphaDeg = inp.alpha
  local v0 = inp.v0
  local g = inp.g or 9.8
  local out = L()
  local alpha = alphaDeg * pi / 180
  add(out, string.format("α = %s°, v₀ = %s, g = %s", num(alphaDeg), num(v0), num(g)))
  local vy = v0 * math.sin(alpha)
  local vx = v0 * math.cos(alpha)
  add(out, string.format("v₀ₓ = v₀cosα = %s·cos(%s°) = %s", num(v0), num(alphaDeg), d4(vx)))
  add(out, string.format("v₀ᵧ = v₀sinα = %s·sin(%s°) = %s", num(v0), num(alphaDeg), d4(vy)))
  add(out, "")
  add(out, "Max height H = (v₀sinα)² / (2g)")
  local H = vy * vy / (2 * g)
  add(out, string.format("  = (%s)² / (2·%s) = %s / %s = %s",
      d4(vy), num(g), d4(vy*vy), num(2*g), d4(H)))
  add(out, "")
  add(out, "Time to max t_up = v₀sinα / g")
  add(out, string.format("  = %s / %s = %s", d4(vy), num(g), d4(vy/g)))
  add(out, "Total flight T = 2·v₀sinα / g = " .. d4(2*vy/g))
  add(out, "")
  add(out, "Range R = v₀²sin(2α) / g")
  local R = v0 * v0 * math.sin(2*alpha) / g
  add(out, string.format("  = %s·sin(%s°) / %s = %s", num(v0*v0), num(2*alphaDeg), num(g), d4(R)))
  return out
end

----------------------------------------------------------------------
-- REFERENCE SCREENS (static step/answer content)
----------------------------------------------------------------------

M.refs = {}

M.refs.regions = {
  "Describing 3D regions (single equations)",
  "RECTANGULAR:",
  "  z = k : horizontal plane     x = k : plane ⟂ x-axis",
  "  x²+y²+z² = a² : sphere radius a",
  "  z = x²+y² : circular paraboloid opening up",
  "CYLINDRICAL (r,θ,z):",
  "  r = a : cylinder radius a about z-axis",
  "  θ = c : half-plane through z-axis",
  "  z = r : cone (half) opening up",
  "SPHERICAL (ρ,θ,φ):",
  "  ρ = a : sphere radius a",
  "  φ = c : cone from origin (half-angle c)",
  "  θ = c : half-plane through z-axis",
  "  φ = π/2 : xy-plane",
}

M.refs.meaningful = {
  "Is the expression meaningful? (scalar s, vector v)",
  "a·b        -> scalar  : MEANINGFUL",
  "a×b        -> vector  : MEANINGFUL",
  "(a·b)·c    -> NOT: dot needs two vectors, a·b is scalar",
  "(a·b)c     -> vector  : MEANINGFUL (scalar times vector)",
  "a×(b·c)    -> NOT: cross needs two vectors, b·c is scalar",
  "(a×b)·c    -> scalar  : MEANINGFUL (triple product)",
  "(a×b)×c    -> vector  : MEANINGFUL",
  "a·(b×c)    -> scalar  : MEANINGFUL (triple product)",
  "|a|·b      -> vector  : MEANINGFUL (scalar |a| times b)",
  "|a|×b      -> NOT: cross needs a vector on the left, |a| is scalar",
  "a + |b|    -> NOT: cannot add vector and scalar",
}

M.refs.truefalse = {
  "True / False review (15):",
  "1. Two lines with no intersection are parallel.          FALSE (skew)",
  "2. a·b = b·a (dot is commutative).                        TRUE",
  "3. a×b = b×a.                                             FALSE (=-b×a)",
  "4. a×a = 0.                                               TRUE",
  "5. a·a = |a|².                                            TRUE",
  "6. If a·b = 0 (a,b≠0) then a ⟂ b.                         TRUE",
  "7. If a×b = 0 (a,b≠0) then a ∥ b.                         TRUE",
  "8. |a×b| = area of parallelogram on a,b.                  TRUE",
  "9. a·(b×c) = (a×b)·c.                                     TRUE",
  "10. The cross product of parallel vectors is 0.           TRUE",
  "11. Projection comp_a b is always ≥ 0.                    FALSE",
  "12. Distance formula needs a unit normal.                 FALSE (any n works)",
  "13. ρ = c is a sphere in spherical coords.                TRUE",
  "14. r = c is a circle in cylindrical coords.              FALSE (cylinder)",
  "15. Three points always determine a unique plane.         FALSE (if collinear)",
}

M.refs.domainrange = {
  "Common domain / range (f(x,y)):",
  "f = √(a²-x²-y²): dom x²+y²≤a²; range [0,a]",
  "f = ln(x+y):     dom x+y>0;   range (-∞,∞)",
  "f = 1/(x-y):     dom x≠y;     range (-∞,0)∪(0,∞)",
  "f = √(x²+y²):    dom all (x,y); range [0,∞)",
  "f = e^{x+y}:     dom all;      range (0,∞)",
  "f = arcsin(x+y): dom -1≤x+y≤1; range [-π/2,π/2]",
  "f = 1/√(x²+y²):  dom (x,y)≠(0,0); range (0,∞)",
}

M.refs.quadrics = {
  "Quadric surfaces (standard forms, traces):",
  "x²/a²+y²/b²+z²/c² = 1  ELLIPSOID (all traces ellipses)",
  "z = x²/a²+y²/b²        ELLIPTIC PARABOLOID (parab up, ellipse H)",
  "z = x²/a²-y²/b²        HYPERBOLIC PARABOLOID (saddle)",
  "x²/a²+y²/b²-z²/c² = 1  HYPERBOLOID 1 SHEET (ellipse H, hyperb V)",
  "-x²/a²-y²/b²+z²/c² = 1 HYPERBOLOID 2 SHEETS",
  "z² = x²/a²+y²/b²       CONE (lines through origin V)",
  "Trace = set x,y,or z = const and identify the 2D curve.",
}

M.refs.matching = {
  "Equation -> graph (surface type):",
  "x²+y²+z² = 4         sphere",
  "x²+y² = 4            cylinder (axis z)",
  "z = x²+y²            paraboloid up",
  "z² = x²+y²           cone",
  "x²+y²-z² = 1         hyperboloid 1 sheet",
  "z²-x²-y² = 1         hyperboloid 2 sheets",
  "z = x²-y²            saddle (hyperbolic paraboloid)",
  "x²/4+y²/9+z² = 1     ellipsoid",
  "y = x²               parabolic cylinder (axis z)",
}

M.refs.formulas = {
  "FORMULA SHEET",
  "Dot:   a·b = |a||b|cosθ = a₁b₁+a₂b₂+a₃b₃",
  "Cross: |a×b| = |a||b|sinθ ; a×b ⟂ a,b",
  "comp_a b = a·b/|a|   proj_a b = (a·b/|a|²)a",
  "Area parallelogram = |a×b|",
  "Volume box = |a·(b×c)|",
  "Line: r = r₀ + t v ; sym (x-x₀)/a=(y-y₀)/b=(z-z₀)/c",
  "Plane: a(x-x₀)+b(y-y₀)+c(z-z₀)=0 ; n=⟨a,b,c⟩",
  "Dist pt-plane = |ax₀+by₀+cz₀+d|/√(a²+b²+c²)",
  "Sphere: center(-D/2,-E/2,-F/2), r²=ΣD/2²-G",
  "Cyl: x=rcosθ, y=rsinθ, z=z ; r²=x²+y²",
  "Sph: x=ρsinφcosθ, y=ρsinφsinθ, z=ρcosφ",
  "     ρ²=x²+y²+z², cosφ=z/ρ",
  "T(t)=r'/|r'|  ; L=∫|r'(t)|dt",
  "Proj: H=(v₀sinα)²/2g, R=v₀²sin2α/g",
  "Unit circle: cos0=1, cos(π/6)=√3/2, cos(π/4)=√2/2,",
  "  cos(π/3)=1/2, cos(π/2)=0 ; sin swaps order",
}

----------------------------------------------------------------------
-- MENU DEFINITION
----------------------------------------------------------------------

-- Each menu entry: title, kind ("solver" or "ref"), key, and (for solvers)
-- a list of input fields {name, label, default?}.
M.menu = {
  { title = "1. Sphere center & radius", kind = "solver", key = "sphere",
    fields = { {"A","coef A (x²)",1}, {"D","D (x term)"}, {"E","E (y term)"},
               {"F","F (z term)"}, {"G","G (const)"} } },
  { title = "2. Describe 3D regions", kind = "ref", key = "regions" },
  { title = "3. Meaningful expression?", kind = "ref", key = "meaningful" },
  { title = "4. True / False review", kind = "ref", key = "truefalse" },
  { title = "5. Vector operations", kind = "solver", key = "vectors",
    fields = { {"a1","a₁"},{"a2","a₂"},{"a3","a₃"},
               {"b1","b₁"},{"b2","b₂"},{"b3","b₃"},
               {"c1","c₁ (opt)"},{"c2","c₂ (opt)"},{"c3","c₃ (opt)"} } },
  { title = "6. Parametric line P,Q", kind = "solver", key = "line",
    fields = { {"p1","P x"},{"p2","P y"},{"p3","P z"},
               {"q1","Q x"},{"q2","Q y"},{"q3","Q z"} } },
  { title = "7. Plane through 3 points", kind = "solver", key = "plane3",
    fields = { {"p1","P x"},{"p2","P y"},{"p3","P z"},
               {"q1","Q x"},{"q2","Q y"},{"q3","Q z"},
               {"r1","R x"},{"r2","R y"},{"r3","R z"} } },
  { title = "8. Two planes angle/∥/⟂", kind = "solver", key = "planes2",
    fields = { {"a1","n₁ a"},{"b1","n₁ b"},{"c1","n₁ c"},
               {"a2","n₂ a"},{"b2","n₂ b"},{"c2","n₂ c"} } },
  { title = "9. Point to plane distance", kind = "solver", key = "ptplane",
    fields = { {"a","plane a"},{"b","plane b"},{"c","plane c"},{"d","plane = d"},
               {"x0","pt x"},{"y0","pt y"},{"z0","pt z"} } },
  { title = "10. Domain & range", kind = "ref", key = "domainrange" },
  { title = "11. Quadric surfaces", kind = "ref", key = "quadrics" },
  { title = "12. Coordinate conversion", kind = "solver", key = "convert",
    fields = { {"mode","mode r2c/c2r/r2s/s2r/c2s/s2c","r2c"},
               {"x","x or r or ρ"},{"y","y or θ"},{"z","z or φ"} } },
  { title = "13. Equation -> graph", kind = "ref", key = "matching" },
  { title = "14. Helix properties", kind = "solver", key = "helix",
    fields = { {"a","amp a"},{"w","ω (ang freq)"},{"c","z slope c"},
               {"t0","t₀",0},{"t1","t₁ (arc end)"} } },
  { title = "15. Particle motion", kind = "solver", key = "particle",
    fields = { {"ax","a_x poly (c0,c1..)"},{"ay","a_y poly"},{"az","a_z poly"},
               {"v0","v(0) x,y,z"},{"r0","r(0) x,y,z"},{"teval","eval t"} } },
  { title = "16. Projectile motion", kind = "solver", key = "projectile",
    fields = { {"alpha","angle α (deg)"},{"v0","speed v₀"},{"g","g",9.8} } },
  { title = "17. Formula sheet", kind = "ref", key = "formulas" },
}

----------------------------------------------------------------------
-- INPUT PARSING (device & test): numbers, negatives, fractions, sqrt, pi
----------------------------------------------------------------------

-- compile "return <expr>" in a way that works on Lua 5.1 and later
local function compileExpr(expr)
  local chunk = "return " .. expr
  if loadstring then return loadstring(chunk) end   -- Lua 5.1 (TI-Nspire)
  return load(chunk)                                -- Lua 5.2+
end

-- parse a single scalar token like "-3", "1/2", "sqrt(2)", "pi", "2*pi/3"
local function parseScalar(s)
  if s == nil then return nil end
  s = tostring(s):gsub("%s+", "")
  if s == "" then return nil end
  local low = s:lower():gsub("π", "pi"):gsub("√", "sqrt")
  -- allow only digits, . + - * / ( ) and the words pi / sqrt
  if not low:gsub("sqrt", ""):gsub("pi", ""):match("^[%d%.%+%-%*/%(%)]*$") then
    return nil
  end
  local expr = low:gsub("pi", "(" .. tostring(pi) .. ")"):gsub("sqrt", "math.sqrt")
  local f = compileExpr(expr)
  if not f then return nil end
  if setfenv then setfenv(f, { math = math }) end   -- sandbox on Lua 5.1
  local ok, v = pcall(f)
  if ok and type(v) == "number" then return v end
  return nil
end
M.parseScalar = parseScalar

-- parse a comma list "1,0,6" -> {1,0,6}
local function parseList(s)
  local t = {}
  if s == nil or tostring(s) == "" then return t end
  for tok in tostring(s):gmatch("[^,]+") do
    t[#t + 1] = parseScalar(tok) or 0
  end
  return t
end
M.parseList = parseList

-- Build a solver-input table from raw string field values for a menu entry.
function M.buildInputs(entry, raw)
  local inp = {}
  local key = entry.key
  if key == "particle" then
    inp.ax = parseList(raw.ax); inp.ay = parseList(raw.ay); inp.az = parseList(raw.az)
    if #inp.ax == 0 then inp.ax = {0} end
    if #inp.ay == 0 then inp.ay = {0} end
    if #inp.az == 0 then inp.az = {0} end
    inp.v0 = parseList(raw.v0); inp.r0 = parseList(raw.r0)
    for i = 1, 3 do inp.v0[i] = inp.v0[i] or 0; inp.r0[i] = inp.r0[i] or 0 end
    if raw.teval and tostring(raw.teval) ~= "" then inp.teval = parseScalar(raw.teval) end
    return inp
  end
  if key == "convert" then
    inp.mode = (raw.mode and tostring(raw.mode) ~= "") and tostring(raw.mode):gsub("%s+","") or "r2c"
    local a, b, c = parseScalar(raw.x), parseScalar(raw.y), parseScalar(raw.z)
    if inp.mode == "r2c" or inp.mode == "r2s" then inp.x, inp.y, inp.z = a, b, c
    elseif inp.mode == "c2r" or inp.mode == "c2s" then inp.r, inp.theta, inp.z = a, b, c
    elseif inp.mode == "s2r" or inp.mode == "s2c" then inp.rho, inp.theta, inp.phi = a, b, c end
    return inp
  end
  for _, f in ipairs(entry.fields) do
    local name = f[1]
    local rawv = raw[name]
    if rawv == nil or tostring(rawv) == "" then
      if f[3] ~= nil then inp[name] = f[3] end       -- default
    else
      inp[name] = parseScalar(rawv)
    end
  end
  return inp
end

-- Run a menu entry and return display lines.
function M.run(entry, raw)
  if entry.kind == "ref" then
    return M.refs[entry.key]
  end
  local inp = M.buildInputs(entry, raw)
  return M.solvers[entry.key](inp)
end

----------------------------------------------------------------------
-- ON-DEVICE UI (TI-Nspire). Skipped automatically off-device (no platform).
----------------------------------------------------------------------

local function startUI()
  local W, H = 318, 212
  local screen = "menu"          -- menu | input | solution
  local menuSel = 1
  local menuTop = 1
  local curEntry = nil
  local fieldVals = {}
  local fieldSel = 1
  local solLines = {}
  local solTop = 1

  local FONT = { "sansserif", "r", 9 }
  local LH = 15                   -- line height

  local function setScreenSize(gc)
    W = platform.window:width()
    H = platform.window:height()
  end

  local function startEntry(entry)
    curEntry = entry
    if entry.kind == "ref" then
      solLines = M.refs[entry.key]
      solTop = 1
      screen = "solution"
    else
      fieldVals = {}
      for _, f in ipairs(entry.fields) do
        fieldVals[f[1]] = (f[3] ~= nil) and tostring(f[3]) or ""
      end
      fieldSel = 1
      screen = "input"
    end
    platform.window:invalidate()
  end

  local function doSolve()
    local raw = {}
    for _, f in ipairs(curEntry.fields) do raw[f[1]] = fieldVals[f[1]] end
    local ok, res = pcall(M.run, curEntry, raw)
    if ok and type(res) == "table" then
      solLines = res
    else
      solLines = { "Error: check inputs.", tostring(res) }
    end
    solTop = 1
    screen = "solution"
    platform.window:invalidate()
  end

  function on.paint(gc)
    setScreenSize(gc)
    gc:setColorRGB(255, 255, 255)
    gc:fillRect(0, 0, W, H)
    gc:setColorRGB(0, 0, 0)
    gc:setFont(unpack(FONT))

    if screen == "menu" then
      gc:setFont("sansserif", "b", 10)
      gc:drawString("Calc3 Exam 1", 6, 2, "top")
      gc:setFont(unpack(FONT))
      local rows = math.floor((H - 22) / LH)
      if menuSel < menuTop then menuTop = menuSel end
      if menuSel > menuTop + rows - 1 then menuTop = menuSel - rows + 1 end
      local y = 20
      for i = menuTop, math.min(#M.menu, menuTop + rows - 1) do
        if i == menuSel then
          gc:setColorRGB(0, 0, 160)
          gc:fillRect(2, y, W - 4, LH)
          gc:setColorRGB(255, 255, 255)
        else
          gc:setColorRGB(0, 0, 0)
        end
        gc:drawString(M.menu[i].title, 6, y, "top")
        y = y + LH
      end
      gc:setColorRGB(0, 0, 0)

    elseif screen == "input" then
      gc:setFont("sansserif", "b", 10)
      gc:drawString(curEntry.title, 6, 2, "top")
      gc:setFont(unpack(FONT))
      local y = 20
      for i, f in ipairs(curEntry.fields) do
        local label = f[2] .. ": " .. (fieldVals[f[1]] or "")
        if i == fieldSel then
          gc:setColorRGB(0, 0, 160)
          gc:fillRect(2, y, W - 4, LH)
          gc:setColorRGB(255, 255, 255)
          gc:drawString(label .. "_", 6, y, "top")
          gc:setColorRGB(0, 0, 0)
        else
          gc:drawString(label, 6, y, "top")
        end
        y = y + LH
      end
      gc:drawString("enter=solve  esc=back", 6, H - 14, "top")

    elseif screen == "solution" then
      local rows = math.floor((H - 16) / LH)
      if solTop < 1 then solTop = 1 end
      if solTop > math.max(1, #solLines - rows + 1) then
        solTop = math.max(1, #solLines - rows + 1)
      end
      local y = 2
      for i = solTop, math.min(#solLines, solTop + rows - 1) do
        gc:drawString(solLines[i] or "", 4, y, "top")
        y = y + LH
      end
      if #solLines > rows then
        gc:drawString("▲▼ scroll  esc=back", 4, H - 14, "top")
      else
        gc:drawString("esc=back", 4, H - 14, "top")
      end
    end
  end

  function on.arrowKey(key)
    if screen == "menu" then
      if key == "up" then menuSel = math.max(1, menuSel - 1)
      elseif key == "down" then menuSel = math.min(#M.menu, menuSel + 1) end
    elseif screen == "input" then
      if key == "up" then fieldSel = math.max(1, fieldSel - 1)
      elseif key == "down" then fieldSel = math.min(#curEntry.fields, fieldSel + 1) end
    elseif screen == "solution" then
      if key == "up" then solTop = solTop - 1
      elseif key == "down" then solTop = solTop + 1 end
    end
    platform.window:invalidate()
  end

  function on.enterKey()
    if screen == "menu" then
      startEntry(M.menu[menuSel])
    elseif screen == "input" then
      doSolve()
    end
  end

  function on.tabKey()
    if screen == "input" then
      fieldSel = (fieldSel % #curEntry.fields) + 1
      platform.window:invalidate()
    end
  end

  function on.escapeKey()
    if screen == "solution" then
      screen = (curEntry and curEntry.kind == "ref") and "menu" or
               (curEntry and curEntry.kind == "solver" and "input") or "menu"
    elseif screen == "input" then
      screen = "menu"
    end
    platform.window:invalidate()
  end

  function on.charIn(ch)
    if screen == "input" then
      local f = curEntry.fields[fieldSel]
      fieldVals[f[1]] = (fieldVals[f[1]] or "") .. ch
      platform.window:invalidate()
    end
  end

  function on.backspaceKey()
    if screen == "input" then
      local f = curEntry.fields[fieldSel]
      local s = fieldVals[f[1]] or ""
      fieldVals[f[1]] = s:sub(1, -2)
      platform.window:invalidate()
    end
  end

  function on.backtabKey()
    if screen == "input" then
      fieldSel = ((fieldSel - 2) % #curEntry.fields) + 1
      platform.window:invalidate()
    end
  end
end

-- Register UI only on device (platform table exists on TI-Nspire).
if platform ~= nil and platform.window ~= nil then
  startUI()
end

return M
