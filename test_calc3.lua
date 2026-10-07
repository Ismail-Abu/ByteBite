-- Desktop test harness for calc3.lua
-- Run with a desktop Lua (5.1-compatible) from this directory:
--     lua5.1 test_calc3.lua        (or luajit / lua)
-- Exits with status 1 if any check fails.
--
-- calc3.lua only starts its TI-Nspire UI when a `platform` table exists,
-- so loading it off-device just returns the module for testing.

local M = assert(dofile("calc3.lua"))

local fails = 0
local function check(name, cond, got)
  if cond then
    print("[PASS] " .. name)
  else
    fails = fails + 1
    print("[FAIL] " .. name .. "  got: " .. tostring(got))
  end
end

local function joined(t) return table.concat(t, "\n") end
local function has(t, s) return joined(t):find(s, 1, true) ~= nil end

----------------------------------------------------------------------
-- exact-form helpers
----------------------------------------------------------------------
check("sqrt 37 -> √37", M.sqrtStr(37) == "√37", M.sqrtStr(37))
check("sqrt 8 -> 2√2",  M.sqrtStr(8)  == "2√2", M.sqrtStr(8))
check("sqrt 4 -> 2",    M.sqrtStr(4)  == "2",   M.sqrtStr(4))
check("frac 4/9",       M.fracStr(4, 9)  == "4/9", M.fracStr(4, 9))
check("frac 8/12->2/3", M.fracStr(8, 12) == "2/3", M.fracStr(8, 12))
check("pi frac 2π/3",   M.piFracStr(2 * math.pi / 3) == "2π/3", M.piFracStr(2 * math.pi / 3))
check("pi frac π/4",    M.piFracStr(math.pi / 4) == "π/4", M.piFracStr(math.pi / 4))

----------------------------------------------------------------------
-- solvers against the exam test cases
----------------------------------------------------------------------
-- (1) Sphere D=-2,E=-2,F=2,G=-1 -> center (1,1,-1), r=2
local s = M.solvers.sphere({ A = 1, D = -2, E = -2, F = 2, G = -1 })
check("sphere center (1,1,-1)", has(s, "(1, 1, -1)"))
check("sphere r=2", has(s, "√4 = 2"))

-- (5) Vectors a=<1,0,6>, b=<2,3,-8>
local v = M.solvers.vectors({ a1 = 1, a2 = 0, a3 = 6, b1 = 2, b2 = 3, b3 = -8 })
check("|a| = √37", has(v, "√37"))
check("a.b = -46", has(v, "= -46"))
check("a×b = <-18,20,3>", has(v, "⟨-18, 20, 3⟩"))
check("scalar proj -7.5624", has(v, "-7.5624"))

-- (6) Line P(-1,0,5), Q(2,-1,3)
local ln = M.solvers.line({ p1 = -1, p2 = 0, p3 = 5, q1 = 2, q2 = -1, q3 = 3 })
check("line dir <3,-1,-2>", has(ln, "⟨3, -1, -2⟩"))
check("line x = -1 + (3)t", has(ln, "x = -1 + (3)t"))

-- (7) Plane P(1,0,2) Q(3,1,-1) R(0,2,4) -> 8x-y+5z=18
local pl = M.solvers.plane3({ p1 = 1, p2 = 0, p3 = 2, q1 = 3, q2 = 1, q3 = -1,
                              r1 = 0, r2 = 2, r3 = 4 })
check("plane normal <8,-1,5>", has(pl, "⟨8, -1, 5⟩"))
check("plane 8x-y+5z=18", has(pl, "= 18"))

-- (8) planes
check("planes perpendicular",
  has(M.solvers.planes2({ a1 = 1, b1 = 0, c1 = 0, a2 = 0, b2 = 1, c2 = 0 }), "PERPENDICULAR"))
check("planes parallel",
  has(M.solvers.planes2({ a1 = 2, b1 = 4, c1 = 6, a2 = 1, b2 = 2, c2 = 3 }), "PARALLEL"))

-- (9) point-plane distance x+2y+2z=1, point origin -> 1/3
check("pt-plane dist 1/3",
  has(M.solvers.ptplane({ a = 1, b = 2, c = 2, d = 1, x0 = 0, y0 = 0, z0 = 0 }), "1/3"))

-- (12) rect (1,1,0) -> cyl : r=√2, θ=π/4
local cv = M.solvers.convert({ mode = "r2c", x = 1, y = 1, z = 0 })
check("convert r=√2", has(cv, "√2"))
check("convert θ=π/4", has(cv, "π/4"))

-- (14) helix a=4,ω=π,c=π -> |r'| = π√17 ≈ 12.9531
check("helix speed 12.9531",
  has(M.solvers.helix({ a = 4, w = math.pi, c = math.pi, t0 = 0, t1 = 1 }), "12.9531"))

-- (16) projectile α=45°, v0=20 -> H=10.2041
check("projectile H=10.2041",
  has(M.solvers.projectile({ alpha = 45, v0 = 20, g = 9.8 }), "10.2041"))

-- (15) particle const accel az=-9.8, v0z=0, r0z=100, t=2 -> vz=-19.6, z=80.4
local pm = M.solvers.particle({ ax = {0}, ay = {0}, az = {-9.8},
                                v0 = {1,0,0}, r0 = {0,0,100}, teval = 2 })
check("particle vz=-19.6", has(pm, "-19.6000"))
check("particle z=80.4", has(pm, "80.4000"))

-- parser
check("parse 1/2",    M.parseScalar("1/2") == 0.5)
check("parse sqrt(2)", math.abs(M.parseScalar("sqrt(2)") - math.sqrt(2)) < 1e-9)
check("parse pi",     math.abs(M.parseScalar("pi") - math.pi) < 1e-9)
check("parse reject", M.parseScalar("os.exit()") == nil)

----------------------------------------------------------------------
print("")
if fails == 0 then
  print("ALL CHECKS PASSED")
  os.exit(0)
else
  print(fails .. " CHECK(S) FAILED")
  os.exit(1)
end
