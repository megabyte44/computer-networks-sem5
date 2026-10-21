# 📻 Experiment 8 — Multicasting (NS2)

> **New concept:** Instead of one sender → one receiver (unicast),
> multicasting lets one sender deliver to **a GROUP** of receivers efficiently.

---

## 🧠 Mental Model

```
UNICAST (normal):     Sender sends 3 separate copies
  A ──────────────► B
  A ──────────────► C    ← 3 separate streams, wastes bandwidth
  A ──────────────► D

MULTICAST:            Network replicates at branch points
  A ──────────► router ──► B
                       └──► C
                       └──► D     ← only 1 stream from A, network splits it!
```

**Real-world example:** Live streaming on YouTube — your ISP doesn't send you 1 million separate copies for 1 million viewers. It uses multicast trees!

---

## 🔑 Key New Concepts

| Concept | What it is |
|---------|-----------|
| `Multicast group` | A virtual address (like a phone conference number) that many nodes join |
| `Node allocaddr` | Creates a unique multicast group address |
| `join-group` | Node subscribes to receive multicast packets |
| `leave-group` | Node unsubscribes from the group |
| `DM (Dense Mode)` | Multicast protocol — floods traffic everywhere, then prunes unreachable branches |
| `mrtproto` | Multicast Routing Protocol |

---

## 📄 multicast.tcl — Fully Annotated

```tcl
# ══════════════════════════════════════
# Note the -multicast on flag!
# ══════════════════════════════════════
set ns [new Simulator -multicast on]   ;# ← MUST enable multicast!

# Trace files
set tf [open output.tr w]
$ns trace-all $tf
set fd [open mcast.nam w]
$ns namtrace-all $fd

# ══════════════════════════════════════
# CREATE NODES — 8 total (n0-n7)
# ══════════════════════════════════════
set n0 [$ns node]    ;# Sender for group1
set n1 [$ns node]    ;# Sender for group2
set n2 [$ns node]    ;# Router
set n3 [$ns node]    ;# Router
set n4 [$ns node]    ;# Router
set n5 [$ns node]    ;# Receiver node
set n6 [$ns node]    ;# Receiver node
set n7 [$ns node]    ;# Receiver node

# ══════════════════════════════════════
# TOPOLOGY
# n0 ─┐
#     n2 ── n3 ─┬─ n4 ─┬─ n5
# n1 ─┘         └─ n7   └─ n6
# ══════════════════════════════════════
$ns duplex-link $n0 $n2 1.5Mb 10ms DropTail
$ns duplex-link $n1 $n2 1.5Mb 10ms DropTail
$ns duplex-link $n2 $n3 1.5Mb 10ms DropTail
$ns duplex-link $n3 $n4 1.5Mb 10ms DropTail
$ns duplex-link $n3 $n7 1.5Mb 10ms DropTail
$ns duplex-link $n4 $n5 1.5Mb 10ms DropTail
$ns duplex-link $n4 $n6 1.5Mb 10ms DropTail

# ══════════════════════════════════════
# MULTICAST PROTOCOL SETUP
# ══════════════════════════════════════
set mproto DM                              ;# Dense Mode multicast
set mrthandle [$ns mrtproto $mproto {}]    ;# activate multicast routing

# ══════════════════════════════════════
# CREATE MULTICAST GROUPS
# ══════════════════════════════════════
set group1 [Node allocaddr]   ;# group1 gets a unique address
set group2 [Node allocaddr]   ;# group2 gets a different unique address

# ══════════════════════════════════════
# SENDERS (attach UDP agents and point to group addresses)
# ══════════════════════════════════════
# Sender for group1 (at node n0)
set udp0 [new Agent/UDP]
$ns attach-agent $n0 $udp0
$udp0 set dst_addr_ $group1    ;# send to group1's address
$udp0 set dst_port_ 0
set cbr1 [new Application/Traffic/CBR]
$cbr1 attach-agent $udp0

# Sender for group2 (at node n1)
set udp1 [new Agent/UDP]
$ns attach-agent $n1 $udp1
$udp1 set dst_addr_ $group2    ;# send to group2's address
$udp1 set dst_port_ 0
set cbr2 [new Application/Traffic/CBR]
$cbr2 attach-agent $udp1

# ══════════════════════════════════════
# RECEIVERS — join and leave groups dynamically!
# ══════════════════════════════════════
# n5 joins group1 at t=1.0
set rcvr1 [new Agent/Null]
$ns attach-agent $n5 $rcvr1
$ns at 1.0 "$n5 join-group $rcvr1 $group1"

# n6 joins group1 at t=1.5
set rcvr2 [new Agent/Null]
$ns attach-agent $n6 $rcvr2
$ns at 1.5 "$n6 join-group $rcvr2 $group1"

# n7 joins group1 at t=2.0
set rcvr3 [new Agent/Null]
$ns attach-agent $n7 $rcvr3
$ns at 2.0 "$n7 join-group $rcvr3 $group1"

# Receivers also join group2:
set rcvr4 [new Agent/Null]
$ns attach-agent $n5 $rcvr4
$ns at 2.5 "$n5 join-group $rcvr4 $group2"
# ... (n6 and n7 also join group2 at later times)

# ══════════════════════════════════════
# NODES LEAVE GROUPS
# ══════════════════════════════════════
$ns at 4.0 "$n5 leave-group $rcvr1 $group1"
$ns at 4.5 "$n6 leave-group $rcvr2 $group1"
$ns at 5.0 "$n7 leave-group $rcvr3 $group1"
# ... etc

# ══════════════════════════════════════
# TRAFFIC SCHEDULE
# ══════════════════════════════════════
$ns at 0.5 "$cbr1 start"    ;# group1 traffic starts
$ns at 9.5 "$cbr1 stop"
$ns at 0.5 "$cbr2 start"    ;# group2 traffic starts
$ns at 9.5 "$cbr2 stop"

$ns at 10.0 "finish"

proc finish {} {
    global ns tf
    $ns flush-trace
    close $tf
    exec nam mcast.nam &
    exit 0
}

$ns set-animation-rate 3.0ms   ;# slow down NAM animation
$ns run
```

---

## 🆚 Unicast vs Multicast in NS2

| Feature | Unicast (normal) | Multicast (this exp) |
|---------|------------------|----------------------|
| Simulator | `new Simulator` | `new Simulator -multicast on` |
| Destination | Specific node | Group address |
| Routing | Standard | `mrtproto DM` |
| Join/Leave | N/A | `join-group` / `leave-group` |
| Efficiency | Wastes bandwidth | Efficient (tree-based delivery) |

---

## 🕐 Timeline of This Simulation

```
t=0.5  → CBR traffic starts for BOTH groups
t=1.0  → n5 joins group1 (starts receiving group1 traffic)
t=1.5  → n6 joins group1
t=2.0  → n7 joins group1
t=2.5  → n5 joins group2 (still in group1 too!)
t=3.0  → n6 joins group2
t=3.5  → n7 joins group2
t=4.0  → n5 leaves group1
t=4.5  → n6 leaves group1
...
t=10.0 → simulation ends
```

---

## ❓ Quick Quiz

1. How do you enable multicast in NS2?
2. What does `Node allocaddr` return?
3. What is Dense Mode (DM) multicast?
4. How does a node start receiving multicast traffic?

<details>
<summary>✅ Answers</summary>

1. `set ns [new Simulator -multicast on]` — add `-multicast on` flag
2. A unique **multicast group address** that nodes can join to receive traffic sent to that address
3. Dense Mode floods traffic everywhere in the network, then **prunes** branches where no receivers exist
4. The node calls `$node join-group $agent $groupAddress` — it subscribes to the group
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Exp 7: DVR & LSR ←](../7_DVR_and_LSR/LEARN.md) | **Exp 8: Multicasting** | [Exp 10 & 11: LAN →](../10_EthernetLAN/LEARN.md) |
