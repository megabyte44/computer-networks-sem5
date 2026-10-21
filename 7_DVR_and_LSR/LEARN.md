# 🗺️ Experiment 7 — Distance Vector Routing (DVR) & Link State Routing (LSR)

> **Core concept:** How do routers decide which PATH to send a packet on?
> Two files: **DV.tcl** (Distance Vector) and **LS.tcl** (Link State).
> The NS2 structure is IDENTICAL — only ONE LINE changes!

---

## 🧠 Mental Model

### DVR (Distance Vector Routing)
```
Each router knows only its NEIGHBORS' distances.
"My neighbor says node 5 is 2 hops from them.
 I'm 1 hop from my neighbor, so node 5 is 3 hops from me."
→ Routers share their TABLE with neighbors (Bellman-Ford algorithm)
→ Slow to converge, may have loops (count-to-infinity problem)
```

### LSR (Link State Routing)
```
Each router knows the COMPLETE MAP of the network.
"I know every link in the network. Let me run Dijkstra's algorithm."
→ Routers broadcast their link states to EVERYONE (flooding)
→ Faster convergence, no loops, but more memory/CPU
```

### The One Line Difference!
```tcl
# DVR:
$ns rtproto DV    ← Distance Vector

# LSR:
$ns rtproto LS    ← Link State
```

---

## 📄 DV.tcl / LS.tcl — Fully Annotated

```tcl
set ns [new Simulator]

# ══════════════════════════════════
# THE KEY LINE — routing protocol!
# ══════════════════════════════════
$ns rtproto DV          ;# ← change to LS for Link State Routing
$ns color 1 green       ;# color traffic flow 1 as green in NAM

# Create 7 nodes in a ring topology
set node0 [$ns node]
set node1 [$ns node]
set node2 [$ns node]
set node3 [$ns node]
set node4 [$ns node]
set node5 [$ns node]
set node6 [$ns node]

# Trace and NAM files
set tf [open out_dv.tr w]    ;# or out_ls.tr for LS
$ns trace-all $tf
set nf [open out_dv.nam w]
$ns namtrace-all $nf
set ft [open "dvr_th" "w"]   ;# throughput output file

# Label nodes for NAM display
$node0 label "node 0"
$node1 label "node 1"
# ... etc

# ══════════════════════════════════
# RING TOPOLOGY — 7 nodes in a circle
# ══════════════════════════════════
# 0 ─── 1 ─── 2 ─── 3 ─── 4 ─── 5 ─── 6
# └──────────────────────────────────────┘ (6 connects back to 0)
$ns duplex-link $node0 $node1 1.5Mb 10ms DropTail
$ns duplex-link $node1 $node2 1.5Mb 10ms DropTail
$ns duplex-link $node2 $node3 1.5Mb 10ms DropTail
$ns duplex-link $node3 $node4 1.5Mb 10ms DropTail
$ns duplex-link $node4 $node5 1.5Mb 10ms DropTail
$ns duplex-link $node5 $node6 1.5Mb 10ms DropTail
$ns duplex-link $node6 $node0 1.5Mb 10ms DropTail  ;# ring closes here!

# Layout nodes in NAM (visual only, not simulation)
$ns duplex-link-op $node0 $node1 orient left-down
# ... etc

# ══════════════════════════════════
# TCP TRAFFIC: node0 → node3 (using FTP)
# ══════════════════════════════════
set tcp2 [new Agent/TCP]
$tcp2 set class_ 1      ;# class 1 = colored green in NAM
$ns attach-agent $node0 $tcp2
set sink2 [new Agent/TCPSink]
$ns attach-agent $node3 $sink2
$ns connect $tcp2 $sink2
set traffic_ftp2 [new Application/FTP]
$traffic_ftp2 attach-agent $tcp2

# ══════════════════════════════════
# RECORD PROCEDURE (same pattern!)
# ══════════════════════════════════
proc record {} {
    global sink2 tf ft
    set ns [Simulator instance]
    set time 0.1
    set now [$ns now]
    set bw0 [$sink2 set bytes_]
    puts $ft "$now [expr $bw0/$time*8/1000000]"    ;# throughput in Mbps
    $sink2 set bytes_ 0
    $ns at [expr $now+$time] "record"
}

# ══════════════════════════════════
# THE INTERESTING PART: Link failures!
# ══════════════════════════════════
$ns at 0.5  "$traffic_ftp2 start"

# At t=2.0: BREAK the link between node2 and node3
$ns rtmodel-at 2.0 down $node2 $node3
# ↑ This simulates a link failure! Now traffic must reroute.
# DVR or LS routing should automatically find an alternate path.

# At t=3.0: RESTORE the link
$ns rtmodel-at 3.0 up $node2 $node3

proc finish {} {
    global ns nf
    $ns flush-trace
    close $nf
    exec nam out_dv.nam &    ;# open animation
    exec xgraph dvr_th &     ;# plot throughput graph
    exit 0
}

$ns at 0.55 "record"
$ns at 5.0 "finish"
$ns run
```

---

## 🔑 The Critical Command

```tcl
$ns rtmodel-at 2.0 down $node2 $node3   ;# link FAILS at t=2s
$ns rtmodel-at 3.0 up   $node2 $node3   ;# link RECOVERS at t=3s
```

This is what makes DVR vs LSR interesting to compare:
- **DVR:** Takes longer to re-route (must gossip changes to all routers)
- **LSR:** Re-routes faster (each router runs Dijkstra with full map)

---

## 🌐 The Ring Topology

```
    node0
   /     \
node6    node1
  |        |
node5    node2
   \     /
    node4─node3
```
**Normal path (node0 → node3):** 0→1→2→3
**After link 2-3 fails:** 0→6→5→4→3 (alternate route around the ring)

---

## 🆚 DVR vs LSR

| Feature | DVR | LSR |
|---------|-----|-----|
| NS2 command | `$ns rtproto DV` | `$ns rtproto LS` |
| Algorithm | Bellman-Ford | Dijkstra |
| Info shared | Distance table (partial) | Link state (full map) |
| Convergence | Slow | Fast |
| Bandwidth usage | Low | Higher (floods full map) |
| Count-to-infinity | Yes (problem) | No |

---

## ❓ Quick Quiz

1. What is the ONE line that changes between DV.tcl and LS.tcl?
2. What does `$ns rtmodel-at 2.0 down $node2 $node3` do?
3. In the ring topology, what is the alternate path when link 2-3 fails?
4. Which is faster to converge after a link failure — DVR or LSR? Why?

<details>
<summary>✅ Answers</summary>

1. `$ns rtproto DV` vs `$ns rtproto LS`
2. Simulates a **link failure** between node2 and node3 at simulation time 2.0 seconds
3. 0 → 6 → 5 → 4 → 3 (going the other way around the ring)
4. **LSR** converges faster because each router has the complete network map and can instantly recalculate paths using Dijkstra
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Exp 6: Flow & Congestion ←](../6_flowcontrol_congestioncontrol/LEARN.md) | **Exp 7: DVR & LSR** | [Exp 8: Multicasting →](../8_Multicasting/LEARN.md) |
