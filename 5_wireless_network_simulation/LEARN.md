# 📶 Experiment 5 — Wireless Network Simulation (NS2)

> **Builds on Exp 4 (Wired NS2)** — Same 5-phase structure, but:
> 1. Nodes are **mobile** (they move around)
> 2. No physical cables — nodes communicate over a **wireless channel**
> 3. Needs extra setup: **topography, GOD, channel, node-config**

---

## 🧠 What's New vs Wired?

```
WIRED (Exp 4)                      WIRELESS (Exp 5)
─────────────────                  ─────────────────────
Nodes fixed in space               Nodes have X,Y coordinates
duplex-link (physical)             wireless channel (radio)
No mobility                        setdest = move to location at speed
No topography needed               Topography (area of simulation)
No GOD needed                      GOD tracks node positions
Simple node-config                 Rich node-config (MAC, antenna, etc.)
```

---

## 📄 wireless.tcl — Phase by Phase

### 🔵 PHASE 0: Set Variables (New in Wireless!)

```tcl
# These variables configure the wireless environment
set val(chan)    Channel/WirelessChannel     ;# radio channel type
set val(prop)   Propagation/TwoRayGround    ;# how radio waves propagate
set val(netif)  Phy/WirelessPhy             ;# physical layer (like WiFi card)
set val(mac)    Mac/802_11                  ;# MAC layer (WiFi standard)
set val(ifq)    Queue/DropTail/PriQueue     ;# queue with priority
set val(ll)     LL                          ;# link layer
set val(ant)    Antenna/OmniAntenna         ;# omnidirectional antenna
set val(ifqlen) 50                          ;# max 50 packets in queue
set val(nn)     6                           ;# 6 nodes
set val(rp)     AODV                        ;# routing protocol (Ad-hoc!)
set val(x)      500                         ;# simulation area: 500m x 500m
set val(y)      500
```

> **Memory trick for val() variables:** "Chan Prop Net Mac Ifq LL Ant → 6 nodes, AODV, 500x500"

### 🔵 PHASE 1 & 2: Simulator + Trace Files

```tcl
set ns [new Simulator]

set tracefile [open wireless.tr w]
$ns trace-all $tracefile

# Note: namtrace-all-WIRELESS needs the area dimensions!
set namfile [open wireless.nam w]
$ns namtrace-all-wireless $namfile $val(x) $val(y)  ;# ← different from wired!
```

### 🔵 PHASE 3a: Topography + GOD (NEW in Wireless)

```tcl
# Topography = defines the physical space (flat grid)
set topo [new Topography]
$topo load_flatgrid $val(x) $val(y)   ;# 500x500 metre flat area

# GOD = General Operations Director
# Tracks node positions + optimizes routing computations
create-god $val(nn)                   ;# one GOD for all 6 nodes
```

### 🔵 PHASE 3b: Create Channels + Configure Nodes

```tcl
# Create wireless channels (separate channels for different groups)
set channel1 [new $val(chan)]
set channel2 [new $val(chan)]
set channel3 [new $val(chan)]

# node-config = tells NS2 what kind of wireless nodes to create
$ns node-config \
  -adhocRouting $val(rp) \    ;# use AODV routing
  -llType       $val(ll) \    ;# link layer
  -macType      $val(mac) \   ;# 802.11 WiFi
  -ifqType      $val(ifq) \   ;# queue type
  -ifqLen       $val(ifqlen) \ ;# queue length
  -antType      $val(ant) \   ;# antenna type
  -propType     $val(prop) \  ;# propagation model
  -phyType      $val(netif) \ ;# physical layer
  -topoInstance $topo \       ;# use the topography we created
  -agentTrace   ON \          ;# log agent events
  -macTrace     ON \          ;# log MAC events
  -routerTrace  ON \          ;# log routing events
  -movementTrace ON \         ;# log movement
  -channel      $channel1     ;# which channel to use
```

### 🔵 PHASE 3c: Create Nodes + Set Positions

```tcl
set n0 [$ns node]
set n1 [$ns node]
set n2 [$ns node]
set n3 [$ns node]
set n4 [$ns node]
set n5 [$ns node]

# Disable random motion (we'll control it manually)
$n0 random-motion 0

# Set initial SIZE in NAM (just visual, not physics)
$ns initial_node_pos $n0 20  ;# node appears as size 20 in NAM

# Set initial X, Y, Z coordinates (Z is always 0 for flat grid)
$n0 set X_ 10.0
$n0 set Y_ 20.0
$n0 set Z_ 0.0

$n1 set X_ 210.0
$n1 set Y_ 230.0
$n1 set Z_ 0.0
# ... etc for n2-n5
```

### 🔵 PHASE 3d: Node Mobility (NEW in Wireless!)

```tcl
# Syntax: $ns at TIME "$node setdest DEST_X DEST_Y SPEED"
$ns at 1.0 "$n1 setdest 490.0 340.0 25.0"   ;# n1 moves to (490,340) at 25 m/s
$ns at 1.0 "$n4 setdest 300.0 130.0 5.0"    ;# n4 moves to (300,130) at 5 m/s
$ns at 1.0 "$n5 setdest 190.0 440.0 15.0"
$ns at 20.0 "$n5 setdest 100.0 200.0 30.0"  ;# n5 changes direction at t=20
```

### 🔵 PHASE 4: TCP + UDP Traffic (Same as Wired!)

```tcl
# TCP traffic: n0 → n5
set tcp  [new Agent/TCP]
set sink [new Agent/TCPSink]
$ns attach-agent $n0 $tcp
$ns attach-agent $n5 $sink
$ns connect $tcp $sink
set ftp [new Application/FTP]
$ftp attach-agent $tcp
$ns at 1.0 "$ftp start"

# UDP traffic: n2 → n3
set udp  [new Agent/UDP]
set null [new Agent/Null]
$ns attach-agent $n2 $udp
$ns attach-agent $n3 $null
$ns connect $udp $null
set cbr [new Application/Traffic/CBR]
$cbr attach-agent $udp
$ns at 1.0 "$cbr start"
```

---

## 🆚 Wired vs Wireless Differences

| Feature | Wired (Exp 4) | Wireless (Exp 5) |
|---------|---------------|------------------|
| Links | `duplex-link` | `node-config` + wireless channel |
| Node position | Not needed | X, Y, Z coordinates required |
| Mobility | None | `setdest X Y speed` |
| Topography | Not needed | `new Topography` + `load_flatgrid` |
| GOD | Not needed | `create-god $val(nn)` |
| NAM trace | `namtrace-all` | `namtrace-all-wireless X Y` |
| Routing | Static (wired) | AODV (dynamic, ad-hoc) |

---

## ❓ Quick Quiz

1. What does `create-god 6` do in wireless NS2?
2. What does `$n1 setdest 490.0 340.0 25.0` mean?
3. Why do wireless nodes need X, Y coordinates?
4. What routing protocol is used and what does AODV stand for?

<details>
<summary>✅ Answers</summary>

1. Creates a **General Operations Director** for 6 nodes — it tracks all node positions for simulation
2. Node n1 will **move to position (490, 340)** at a **speed of 25 m/s** starting at t=1.0s
3. Wireless is based on physical distance — nodes need to know where they are to determine if they're in radio range
4. **AODV** = Ad-hoc On-Demand Distance Vector — discovers routes only when needed
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Exp 4: Wired NS2 ←](../4_wired_network_simulation/LEARN.md) | **Exp 5: Wireless NS2** | [Exp 6: Flow & Congestion →](../6_flowcontrol_congestioncontrol/LEARN.md) |
