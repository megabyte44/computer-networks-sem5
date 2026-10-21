# 🔌 Experiment 4 — Wired Network Simulation (NS2)

> **New tool: NS2 (Network Simulator 2)**
> Forget Java — now you write TCL scripts to simulate entire networks.

---

## 🧠 The NS2 Mental Model (Memorize This!)

```
Every NS2 script = 5 phases:

1. CREATE SIMULATOR      set ns [new Simulator]
       │
2. OPEN LOG FILES        open .tr and .nam files
       │
3. BUILD TOPOLOGY        create nodes + connect with duplex-link
       │
4. ADD TRAFFIC           create agents (TCP/UDP) + applications (FTP/CBR)
       │
5. SCHEDULE & RUN        $ns at X "event"  →  $ns run
```

**Think of NS2 like building with LEGO:**
- **Nodes** = computers
- **Links** = cables (with bandwidth and delay)
- **Agents** = transport protocols (TCP/UDP)
- **Applications** = what runs on top (FTP, CBR)
- **Trace file** = recording of everything that happened
- **NAM file** = animation you can watch

---

## 📄 wired.tcl — Fully Annotated

```tcl
# ═══════════════════════════════════════════
# PHASE 1: Create the simulator engine
# ═══════════════════════════════════════════
set ns [new Simulator]

# ═══════════════════════════════════════════
# PHASE 2: Open log files
# ═══════════════════════════════════════════
set tracefile [open wired.tr w]    ;# .tr = trace (text log of every packet)
$ns trace-all $tracefile

set namfile [open wired.nam w]     ;# .nam = for NAM animator (visual playback)
$ns namtrace-all $namfile

# ═══════════════════════════════════════════
# PHASE 3: Create nodes
# ═══════════════════════════════════════════
set n0 [$ns node]   ;# Creates node 0
set n1 [$ns node]   ;# Creates node 1
set n2 [$ns node]
set n3 [$ns node]
set n4 [$ns node]
set n5 [$ns node]

# Connect nodes with duplex (bidirectional) links
# Syntax: duplex-link NodeA NodeB Bandwidth Delay QueueType
$ns duplex-link $n0 $n1 5Mb  2ms  DropTail  ;# 5 Mbps, 2ms delay
$ns duplex-link $n2 $n1 10Mb 5ms  DropTail
$ns duplex-link $n1 $n4 3Mb  10ms DropTail
$ns duplex-link $n4 $n3 100Mb 2ms DropTail
$ns duplex-link $n4 $n5 4Mb  10ms DropTail

# ═══════════════════════════════════════════
# PHASE 4: Create agents + applications
# ═══════════════════════════════════════════

# --- UDP Traffic: n0 → n3 ---
set udp  [new Agent/UDP]       ;# UDP sender
set null [new Agent/Null]      ;# UDP receiver (just discards packets)
$ns attach-agent $n0 $udp      ;# attach sender to node 0
$ns attach-agent $n3 $null     ;# attach receiver to node 3
$ns connect $udp $null         ;# connect them

# --- TCP Traffic: n2 → n5 ---
set tcp  [new Agent/TCP]       ;# TCP sender
set sink [new Agent/TCPSink]   ;# TCP receiver (handles ACKs)
$ns attach-agent $n2 $tcp
$ns attach-agent $n5 $sink
$ns connect $tcp $sink

# --- Applications ---
set cbr [new Application/Traffic/CBR]   ;# CBR = Constant Bit Rate (like video stream, uses UDP)
$cbr attach-agent $udp

set ftp [new Application/FTP]           ;# FTP = File Transfer (uses TCP)
$ftp attach-agent $tcp

# ═══════════════════════════════════════════
# PHASE 5: Schedule events + run
# ═══════════════════════════════════════════
$ns at 1.0 "$cbr start"    ;# CBR starts at t=1.0s
$ns at 2.0 "$ftp start"    ;# FTP starts at t=2.0s
$ns at 10.0 "finish"       ;# call finish proc at t=10s

proc finish {} {
    global ns tracefile namfile
    $ns flush-trace          ;# write all buffered trace data to file
    close $tracefile
    close $namfile
    exit 0
}

puts "Simulation is starting..."
$ns run                     ;# START THE SIMULATION!
```

---

## 🔑 Key Vocabulary (Must Know!)

| Term | Meaning |
|------|---------|
| `duplex-link A B 5Mb 2ms DropTail` | Two-way link between A and B, 5Mbps, 2ms delay, DropTail queue |
| `DropTail` | When buffer is full, drop the newest packet (tail) |
| `Agent/UDP` | UDP sender agent |
| `Agent/Null` | UDP receiver — just drops/absorbs packets |
| `Agent/TCP` | TCP sender agent |
| `Agent/TCPSink` | TCP receiver — sends ACKs back |
| `Application/Traffic/CBR` | Generates constant rate UDP traffic |
| `Application/FTP` | Generates TCP file transfer traffic |
| `.tr` file | Text trace — one line per packet event |
| `.nam` file | Animation file — open with `nam` tool |

---

## 🌐 Network Topology in This Experiment

```
n0 ──5Mb──  n1 ──3Mb──  n4 ──100Mb── n3
n2 ──10Mb──↗         └──4Mb──── n5
```

- **UDP/CBR:** n0 → n3 (goes through n1 → n4)
- **TCP/FTP:** n2 → n5 (goes through n1 → n4)

---

## 📄 analyse.awk — Fully Annotated

AWK reads the `.tr` trace file **line by line** and counts packets.

### What does a trace file line look like?

```
r  0.5  _0_  _3_  ---  0  tcp  1000  [...]
↑   ↑                   ↑   ↑    ↑
$1  $2                  $5  $6   $8
event  time           flags proto  size
```

| Field | `$N` | Meaning |
|-------|------|---------|
| Event | `$1` | `s`=send, `r`=receive, `d`=drop, `+`=enqueue, `-`=dequeue |
| Time  | `$2` | Simulation time in seconds |
| Proto | `$5` | Protocol: `tcp`, `udp`, `cbr`, `ack` |
| Size  | `$6` | Packet size in bytes |

---

### analyse.awk — Line by Line

```awk
BEGIN {           # runs ONCE before reading any line
    send=0;       # counter: packets sent
    received=0;   # counter: packets received
    dropped=0;    # counter: packets dropped
    start=1.0;    # simulation start time (when CBR started)
    stop=3.0;     # simulation stop time (analysis window)
}

{                         # runs for EVERY line in the trace file
    if($1=="+") {         # "+" = packet enqueued = counted as "sent"
        send++;
    }

    if($5=="tcp") {       # only look at TCP packets
        if($1=="r") {     # "r" = received at destination
            received++;
        }
    }

    if($1=="d") {         # "d" = dropped by a queue
        dropped++;
    }
}

END {             # runs ONCE after all lines are processed
    if(send=="0" && received=="0") {
        print "empty trace file"    # safety check
    }

    print "Number of Packets Received  " received
    print "Throughput =" (received*8)/(stop-start) "bits per second"
    #                         ↑           ↑
    #              bytes → bits (*8)   duration = stop - start
    print "Number of Packets Dropped = " dropped
}
```

### Throughput Formula Explained

```
Throughput = (received packets × 8 bits) ÷ (stop - start seconds)
           = (7190 × 8) ÷ (3.0 - 1.0)
           = 57520 ÷ 2
           = 28760 bits/second  ≈ 28.7 Kbps
```

---

## ▶️ How to Run

```bash
ns wired.tcl                    # step 1: run simulation → creates wired.tr and wired.nam
awk -f analyse.awk wired.tr     # step 2: analyse the trace file
nam wired.nam                   # step 3: open animation (optional)
```

**Expected output:**
```
Number of Packets Received  7190
Throughput = 28760 bits per second
Number of Packets Dropped = 0
```

## ❓ Quick Quiz

1. What does `$ns trace-all $tracefile` do?
2. What's the difference between `Agent/Null` and `Agent/TCPSink`?
3. What does `DropTail` mean as a queue type?
4. Which application goes on top of TCP? Which goes on top of UDP?

<details>
<summary>✅ Answers</summary>

1. Tells NS2 to log every packet event to the trace file (for later analysis)
2. `Agent/Null` just discards UDP packets. `Agent/TCPSink` properly receives TCP data and sends ACKs
3. When the queue buffer is full, the newest arriving packet is **dropped** (from the tail)
4. FTP → TCP. CBR → UDP.
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Exp 9: DHCP ←](../9_DHCP/LEARN.md) | **Exp 4: Wired NS2** | [Exp 5: Wireless NS2 →](../5_wireless_network_simulation/LEARN.md) |
