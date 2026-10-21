# 📖 TCL & AWK Syntax Reference — NS2 Simulations

> One doc to rule them all. Every syntax that appears across all your TCL and AWK files, explained plainly.

---

## 📑 Table of Contents
1. [TCL Basics](#1--tcl-basics)
2. [NS2 Setup — Every Simulation Starts Here](#2--ns2-setup--every-simulation-starts-here)
3. [Nodes](#3--nodes)
4. [Links](#4--links)
5. [Agents (Transport Layer)](#5--agents-transport-layer)
6. [Applications (Traffic Sources)](#6--applications-traffic-sources)
7. [Scheduling Events](#7--scheduling-events)
8. [Procedures (proc)](#8--procedures-proc)
9. [Wireless-Only Syntax](#9--wireless-only-syntax)
10. [Multicast-Only Syntax](#10--multicast-only-syntax)
11. [DVR/LSR-Only Syntax](#11--dvrlsr-only-syntax)
12. [Throughput Measurement (record proc)](#12--throughput-measurement-record-proc)
13. [AWK Basics](#13--awk-basics)
14. [Trace File Format (.tr) — What AWK Reads](#14--trace-file-format-tr--what-awk-reads)
15. [AWK Patterns Used Across All Experiments](#15--awk-patterns-used-across-all-experiments)
16. [Quick Lookup Tables](#16--quick-lookup-tables)

---

## 1. 🔤 TCL Basics

```tcl
# This is a comment
set x 10               # create a variable x = 10
puts $x                # print x  ($ = read the variable)
puts "hello world"     # print a string

set result [expr 5+3]  # expr = evaluate math  → result = 8
puts $result           # prints 8

# String in double quotes: variables are expanded
# String in single quotes (braces): treated literally
puts "value is $x"     # prints: value is 10
puts {value is $x}     # prints: value is $x  (literal)
```

> **Key rule:** `$x` reads the variable. `set x val` writes it. `[cmd]` runs a command and returns its output.

---

## 2. 🔧 NS2 Setup — Every Simulation Starts Here

```tcl
# Create the simulator (the main NS2 object)
set ns [new Simulator]
```
> `new Simulator` creates the NS2 engine. `set ns` saves it so we can call methods on it with `$ns`.

```tcl
# TRACE FILE — logs every packet event
set tracefile [open wired.tr w]     # open file "wired.tr" for Writing
$ns trace-all $tracefile             # tell ns: log all events to this file
```
> `.tr` file is what AWK scripts read later.

```tcl
# NAM FILE — for the visual animation
set namfile [open wired.nam w]
$ns namtrace-all $namfile            # wired simulation
$ns namtrace-all-wireless $namfile $val(x) $val(y)  # wireless (needs dimensions)
```
> NAM = Network Animator. Opens with `exec nam file.nam &`

```tcl
# Extra output files (throughput graphs)
set ft [open "Throughput" "w"]       # create file for xgraph
set fb [open "Bandwidth" "w"]
```

```tcl
# Coloring flows/nodes in NAM (optional)
$ns color 1 green     # flow with color ID 1 = green
$ns color 3 Green
```

```tcl
# Start the simulation (always the LAST line)
$ns run
```

---

## 3. 🔵 Nodes

```tcl
# Basic node creation (wired)
set n0 [$ns node]
set n1 [$ns node]

# Node in a loop (cleaner for many nodes)
for {set i 0} {$i < 6} {incr i} {
    set n($i) [$ns node]        # creates n(0), n(1), ... n(5)
}
# Access them as: $n(0), $n(2), etc.
```

```tcl
# Label a node (shown in NAM)
$node0 label "node 0"

# Color a node at a specific time
$ns at 0.5 "$n(0) color \"green\""
$ns at 0.5 "$node0 label-color green"
```

---

## 4. 🔗 Links

```tcl
# duplex-link = both directions (A→B and B→A)
$ns duplex-link $n0 $n1 5Mb 2ms DropTail
#                          ↑    ↑    ↑
#                     bandwidth  delay  queue type
```

### Queue Types
| Queue | Meaning |
|-------|---------|
| `DropTail` | Normal FIFO queue — drops packets from the tail (end) when full |
| `RED` | Random Early Detection — starts dropping packets BEFORE the queue is full, to prevent congestion |
| `Queue/DropTail/PriQueue` | DropTail with priority (used in wireless) |

```tcl
# Visual layout in NAM (orient = which direction the link points)
$ns duplex-link-op $n0 $n1 orient right
$ns duplex-link-op $n1 $n2 orient right-down
$ns duplex-link-op $n0 $n3 orient left-down
# Options: right, left, up, down, right-up, right-down, left-up, left-down
```

```tcl
# LAN (Ethernet) — connects multiple nodes to a shared medium
$ns make-lan "$n1 $n2 $n3 $n4 $n5 $n6" 0.2Mb 20ms LL Queue/DropTail Mac/802_3
#                                              ↑         ↑         ↑
#                                          bandwidth   delay   link layer   queue   MAC protocol
# Mac/802_3 = Ethernet CSMA/CD
```

---

## 5. 📡 Agents (Transport Layer)

Agents = the transport protocol (TCP/UDP). They sit ON a node and handle sending/receiving.

```tcl
# --- UDP (no connection, no guarantee) ---
set udp  [new Agent/UDP]
set null [new Agent/Null]     # Null = receiver that just discards packets
$ns attach-agent $n0 $udp    # attach sender to node n0
$ns attach-agent $n3 $null   # attach receiver to node n3
$ns connect $udp $null        # create the logical connection

# --- TCP (reliable, connection-oriented) ---
set tcp  [new Agent/TCP]
set sink [new Agent/TCPSink]  # TCPSink = the TCP receiver
$ns attach-agent $n2 $tcp
$ns attach-agent $n5 $sink
$ns connect $tcp $sink

# --- TCP Variants (for congestion control experiment) ---
set tcp1 [new Agent/TCP/Vegas]     # Vegas: delay-based congestion control
set tcp2 [new Agent/TCP/Reno]      # Reno: loss-based, fast retransmit
set tcp3 [new Agent/TCP/Newreno]   # NewReno: improved Reno
set tcp4 [new Agent/TCP/Sack1]     # SACK: Selective Acknowledgement
# default Agent/TCP = Tahoe (original, oldest TCP)

# Assign a flow class (for coloring in NAM)
$tcp2 set class_ 1
```

### Reading bytes received (for throughput calculation)
```tcl
set bw [$sink set bytes_]     # how many bytes received since last reset
$sink set bytes_ 0            # reset counter to 0
```

---

## 6. 📦 Applications (Traffic Sources)

Applications = what generates the actual data/packets. They attach to an Agent.

```tcl
# CBR — Constant Bit Rate (steady stream, like streaming audio)
set cbr [new Application/Traffic/CBR]
$cbr attach-agent $udp

# FTP — File Transfer (sends as fast as TCP allows)
set ftp [new Application/FTP]
$ftp attach-agent $tcp

# Exponential — bursty traffic (on/off pattern, like HTTP)
set http [new Application/Traffic/Exponential]
$http attach-agent $tcp
```

> **Rule:** Application → attached to → Agent → attached to → Node

---

## 7. ⏱️ Scheduling Events

```tcl
$ns at 1.0 "$cbr start"          # at time 1.0s, start CBR traffic
$ns at 2.0 "$ftp start"          # at time 2.0s, start FTP
$ns at 3.2 "$http stop"          # stop HTTP at 3.2s
$ns at 10.0 "finish"             # at 10s, call the finish procedure

# Strings in quotes are executed as TCL commands at that time
$ns at 0.5 "$ns trace-annotate \"Starting FTP\""   # add a note in trace
$ns at 0.5 "$n0 color \"green\""                    # color a node green at 0.5s
```

### Link failure and recovery (DVR experiment)
```tcl
$ns rtmodel-at 2.0 down $node2 $node3   # at 2.0s, bring link n2-n3 DOWN
$ns rtmodel-at 3.0 up   $node2 $node3   # at 3.0s, bring it back UP
```

### Set animation speed
```tcl
$ns set-animation-rate 3.0ms    # slow down NAM animation
```

---

## 8. 🔧 Procedures (proc)

```tcl
proc finish {} {
    global ns tracefile namfile    # "global" = bring these outer variables into scope
    $ns flush-trace                # force-write any buffered trace data
    close $tracefile               # close the .tr file
    close $namfile                 # close the .nam file
    exec nam wired.nam &           # run NAM animator (& = in background)
    exec xgraph Throughput &       # open xgraph with the throughput data file
    exit 0                         # end the simulation
}
```

> `global` is required inside `proc` to access variables defined outside. Without it, TCL cannot see `$ns` etc.

```tcl
proc record {} {
    global ns ft null              # bring in what we need
    set time 0.1                   # sampling interval in seconds
    set now [$ns now]              # get current simulation time
    set bw [$null set bytes_]      # bytes received in last interval
    puts $ft "$now [expr $bw/$time*8/1000000]"   # write: time  Mbps
    #                                  ↑ bytes ÷ time × 8 = bits, ÷ 1000000 = Megabits
    $null set bytes_ 0             # reset byte counter
    $ns at [expr $now+$time] "record"  # schedule itself again 0.1s later (self-loop!)
}
```
> `record` is the key pattern for **live throughput graphs** — it calls itself every 0.1s.

---

## 9. 📶 Wireless-Only Syntax

### Step 1 — Configuration Variables (`val` array)
```tcl
set val(chan)    Channel/WirelessChannel      # wireless medium
set val(prop)   Propagation/TwoRayGround     # radio signal propagation model
set val(netif)  Phy/WirelessPhy              # physical layer (WiFi 2.4GHz)
set val(mac)    Mac/802_11                   # MAC protocol (WiFi)
set val(ifq)    Queue/DropTail/PriQueue      # interface queue
set val(ll)     LL                           # link layer
set val(ant)    Antenna/OmniAntenna          # omnidirectional antenna
set val(ifqlen) 50                           # max packets in queue
set val(nn)     6                            # number of nodes
set val(rp)     AODV                         # routing protocol
set val(x)      500                          # area width in metres
set val(y)      500                          # area height in metres
```

### Step 2 — Topology and GOD
```tcl
set topo [new Topography]
$topo load_flatgrid $val(x) $val(y)    # define a flat 500x500m area

create-god $val(nn)                    # GOD = General Operations Director
                                       # tracks node positions, needed by NS2 internally
```

### Step 3 — Channels (shared wireless medium)
```tcl
set channel1 [new $val(chan)]     # each node group gets a channel
set channel2 [new $val(chan)]
```

### Step 4 — Node Configuration (must be done BEFORE creating nodes)
```tcl
$ns node-config \
  -adhocRouting $val(rp) \        # routing protocol (AODV)
  -llType $val(ll) \              # link layer
  -macType $val(mac) \            # MAC (802.11)
  -ifqType $val(ifq) \            # queue type
  -ifqLen $val(ifqlen) \          # queue length
  -antType $val(ant) \            # antenna
  -propType $val(prop) \          # propagation model
  -phyType $val(netif) \          # physical interface
  -topoInstance $topo \           # attach topology
  -agentTrace ON \                # trace agent-level events
  -macTrace ON \                  # trace MAC-level events
  -routerTrace ON \               # trace routing events
  -movementTrace ON \             # trace node movement
  -channel $channel1              # which channel to use
# The \ at end of line = line continuation (the command continues next line)
```

### Step 5 — Node Positions and Mobility
```tcl
# Set initial X,Y,Z position (Z is always 0 for 2D simulation)
$n0 set X_ 10.0
$n0 set Y_ 20.0
$n0 set Z_ 0.0

# Disable random movement
$n0 random-motion 0

# Set node size in NAM
$ns initial_node_pos $n0 20      # node n0 displayed as size 20 in NAM

# Move a node at runtime: time, node, destination X, Y, Speed(m/s)
$ns at 1.0 "$n1 setdest 490.0 340.0 25.0"
#                              ↑      ↑    ↑
#                          dest X  dest Y  speed in m/s
```

---

## 10. 📻 Multicast-Only Syntax

```tcl
# Enable multicast when creating simulator
set ns [new Simulator -multicast on]

# Set multicast routing protocol (Dense Mode)
set mproto DM
set mrthandle [$ns mrtproto $mproto {}]

# Allocate multicast group addresses
set group1 [Node allocaddr]
set group2 [Node allocaddr]

# Point a UDP agent at a multicast group (instead of a specific node)
$udp0 set dst_addr_ $group1
$udp0 set dst_port_ 0

# Receivers JOIN and LEAVE groups at specific times
$ns at 1.0 "$n5 join-group  $rcvr1 $group1"   # n5 starts receiving group1
$ns at 4.0 "$n5 leave-group $rcvr1 $group1"   # n5 stops receiving group1
```

---

## 11. 🗺️ DVR/LSR-Only Syntax

```tcl
# Set routing protocol BEFORE creating nodes
$ns rtproto DV    # DV = Distance Vector (DVR)
$ns rtproto LS    # LS = Link State (LSR / Dijkstra)

# Label nodes for display
$node0 label "node 0"

# Link failure / recovery (simulates a network topology change)
$ns rtmodel-at 2.0 down $node2 $node3    # bring link down at t=2.0
$ns rtmodel-at 3.0 up   $node2 $node3   # restore link at t=3.0
```

---

## 12. 📊 Throughput Measurement (record proc pattern)

This pattern appears in Exp 4, 6, 7. Memorize it.

```tcl
proc record {} {
    global sink ft             # sink = TCPSink agent, ft = output file handle
    set ns [Simulator instance]    # get the current simulator (inside proc)
    set time 0.1                   # check every 0.1 seconds
    set now [$ns now]              # current simulation time
    set bw [$sink set bytes_]      # bytes received in this interval
    
    # Write "time throughput_in_Mbps" to the file
    puts $ft "$now [expr $bw/$time*8/1000000]"
    #                    ↑ bytes → bits (*8) → Megabits (/1000000)
    
    $sink set bytes_ 0                      # reset counter
    $ns at [expr $now+$time] "record"       # schedule next call
}

# Kick it off at 0.5s
$ns at 0.5 "record"
```

---

## 13. 📝 AWK Basics

AWK reads a file **line by line** and processes each line automatically.

```
Structure of an AWK program:
┌─────────────────────────────────────────┐
│  BEGIN { ... }   runs once at the START │
│                                         │
│  { ... }         runs for EVERY line    │
│                                         │
│  END { ... }     runs once at the END   │
└─────────────────────────────────────────┘
```

```awk
# $0 = the whole line
# $1 = first word (field 1)
# $2 = second word (field 2)
# $N = Nth word
# Fields are split by whitespace by default

BEGIN {
    count = 0         # initialize variables here
}
{
    count++           # runs for every line
    print $1, $2      # print field 1 and field 2
}
END {
    print count       # total lines processed
}
```

### AWK Math and Logic
```awk
x += 5          # same as x = x + 5
x++             # same as x = x + 1
if ($1 == "r")  # string comparison (double ==)
if ($6 > 512)   # number comparison

# Ternary (shorthand if-else)
result = (condition) ? valueIfTrue : valueIfFalse
# Example from wireless.awk:
avgDelay = (validPackets > 0) ? (totalDelay / validPackets) * 1000 : 0
```

### AWK Output
```awk
print "Hello"                          # print with newline
print "value = " x                     # concatenate with space
printf("%.2f\n", x)                    # formatted output (like C printf)
printf("Throughput: %f\n", throughput) # %f = float, %d = int, %s = string
```

---

## 14. 📄 Trace File Format (.tr) — What AWK Reads

Every line in the `.tr` file looks like this:

```
s  0.5  _0_  AGT  ---  1  tcp  1000  [...]  [...]
↑   ↑    ↑    ↑         ↑   ↑    ↑
$1  $2   $4   $5(layer) $6  $7  packet_size
```

| Field | Variable | Meaning |
|-------|----------|---------|
| `$1` | Event type | `s`=send, `r`=receive, `d`=drop, `+`=enqueue, `-`=dequeue |
| `$2` | Time | Simulation time in seconds |
| `$3` | From node | Source node ID |
| `$4` | To node / Layer | Destination node ID or `AGT` (application layer) |
| `$5` | Flags | Usually `---` |
| `$6` | Flow/Seq ID | Packet sequence number or flow ID |
| `$7` | Protocol | `tcp`, `udp`, `cbr`, `ack` |
| `$8` | Packet size | Size in bytes |

### Common AWK filters based on trace fields

```awk
$1 == "s"            # sent event
$1 == "r"            # received event
$1 == "d"            # dropped event
$1 == "+"            # enqueued (entered queue)
$1 == "-"            # dequeued (left queue)

$4 == "AGT"          # Application layer events (wireless trace)
$5 == "tcp"          # TCP packets (wired trace, field positions shift)
$7 == "tcp"          # TCP packets (another format)
$8 > 512             # packet size greater than 512 bytes
$4 == 7              # flow ID 7 (HTTP traffic in flow control exp)
$4 == 5              # flow ID 5 (congestion control exp)
```

---

## 15. 🔍 AWK Patterns Used Across All Experiments

### Pattern 1 — Count send/receive/drop (Exp 4 wired)
```awk
if ($1 == "+")  send++       # packet enqueued = sent
if ($1 == "r" && $5 == "tcp") received++
if ($1 == "d")  dropped++
```

### Pattern 2 — End-to-End Delay using timestamps (Exp 5 wireless)
```awk
# When packet is SENT at application layer → record the send time
if ($4 == "AGT" && $1 == "s") {
    start_time[$6] = $2       # key = packet sequence num, value = send time
}

# When packet is RECEIVED at application layer → compute delay
if ($4 == "AGT" && $1 == "r") {
    totalDelay += $2 - start_time[$6]   # arrival_time - send_time
    receivedPackets++
}
```

### Pattern 3 — Throughput by accumulating data bytes (Exp 6, 7)
```awk
if ($1 == "r" && $4 == flowID) {
    data += $6        # accumulate received packet sizes

    if (flag == 0) {  # record start time (first received packet)
        start = $2
        flag = 1
    }
    end = $2          # keep updating end time (last received packet)
}
# END: throughput = total_bytes / (end - start)
throughput = data / (end - start)
```

### Pattern 4 — Packet Delivery Ratio (Exp 5 wireless)
```awk
# Track max sequence number to find how many were generated
seqno = $6 > seqno ? $6 : seqno    # max(current, previous)

# In END:
generatedPackets = seqno + 1
PDR = (receivedPackets / generatedPackets) * 100
```

### Metrics Summary Table

| Metric | Formula |
|--------|---------|
| **Throughput** | `(received_bytes × 8) / duration` in bits/sec |
| **Throughput (Mbps)** | `(bytes × 8) / (time × 1000000)` |
| **Packet Delivery Ratio** | `(received / generated) × 100` % |
| **Average End-to-End Delay** | `totalDelay / validPackets × 1000` ms |
| **Packet Loss** | `generated - received` |

---

## 16. 📋 Quick Lookup Tables

### TCL — All NS2 Commands at a Glance

| Command | What it does |
|---------|-------------|
| `set ns [new Simulator]` | Create NS2 engine |
| `$ns node` | Create a node |
| `$ns duplex-link n0 n1 BW delay Q` | Connect two nodes |
| `$ns duplex-link-op n0 n1 orient X` | Set visual direction |
| `$ns make-lan "n1 n2 ..." BW delay LL Q MAC` | Create Ethernet LAN |
| `$ns attach-agent n agent` | Attach agent to node |
| `$ns connect agent1 agent2` | Connect two agents |
| `$ns at T "cmd"` | Schedule command at time T |
| `$ns run` | Start simulation |
| `$ns now` | Get current simulation time |
| `$ns flush-trace` | Write buffered trace to file |
| `$ns trace-all file` | Log all events to file |
| `$ns namtrace-all file` | Log NAM animation |
| `$ns rtproto DV/LS` | Set routing protocol |
| `$ns rtmodel-at T up/down n1 n2` | Bring link up/down |
| `$ns color N colorname` | Assign color to flow ID N |
| `$ns trace-annotate "msg"` | Add label in NAM |
| `proc name {} { ... }` | Define a procedure |
| `global varname` | Import outer variable into proc |
| `exec cmd &` | Run a shell command in background |
| `close $file` | Close a file handle |
| `puts $file "text"` | Write to file |
| `[expr math]` | Evaluate math expression |
| `incr i` | i++ (increment) |

### AWK — Quick Reference

| Syntax | Meaning |
|--------|---------|
| `$1`, `$2` ... `$N` | Field number (word) in current line |
| `$0` | Entire current line |
| `BEGIN { }` | Runs once before any lines |
| `{ }` | Runs for every line |
| `END { }` | Runs once after all lines |
| `if (cond) { }` | Conditional |
| `var++` / `var+=x` | Increment / add |
| `print x` | Print with newline |
| `printf("fmt", x)` | Formatted print |
| `array[key] = val` | Associative array |

### Trace Event Types ($1)

| Symbol | Event |
|--------|-------|
| `s` | Packet sent (from application layer) |
| `r` | Packet received |
| `d` | Packet dropped |
| `+` | Packet enqueued |
| `-` | Packet dequeued |
| `D` (capital) | Wireless drop |

---

*This file covers: wired.tcl, wireless.tcl, flow_control.tcl, congestion_control.tcl, DV.tcl, LS.tcl, multicast.tcl, ethernet_lan.tcl, and all their AWK analysis files.*
