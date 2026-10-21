# 🔌 Experiments 10 & 11 — Ethernet LAN & Wireless LAN (NS2)

> **The final experiments.** These simulate shared-medium LANs:
> - **Exp 10:** Ethernet (wired, 802.3, CSMA/CD)
> - **Exp 11:** Wireless LAN (WiFi, 802.11, CSMA/CA)

---

## 🧠 Mental Model

```
LAN = Local Area Network = multiple devices sharing ONE medium

Wired LAN (Ethernet):        Wireless LAN (WiFi):
Devices share one cable      Devices share one radio channel
Uses CSMA/CD                 Uses CSMA/CA
(Detect collision,           (Avoid collision,
 retransmit)                  wait for clear channel)
```

---

## 📄 Experiment 10 — Ethernet LAN (ethernet_lan.tcl)

### The ONE Special Command You Need to Know

```tcl
# Instead of duplex-link between pairs, use make-lan for shared medium:
$ns make-lan "$n1 $n2 $n3 $n4 $n5 $n6" 0.2Mb 20ms LL Queue/DropTail Mac/802_3
#             ↑ list of ALL nodes on LAN   ↑bw  ↑delay ↑link  ↑queue    ↑MAC (Ethernet!)
```

> **`make-lan` = all nodes share the same medium.** This is unlike `duplex-link` which is point-to-point!

### Full Code

```tcl
set ns [new Simulator]

set tr [open "LAN.tr" w]
$ns trace-all $tr
set nam [open "LAN.nam" w]
$ns namtrace-all $nam

# Create 6 nodes
set n1 [$ns node]
set n2 [$ns node]
set n3 [$ns node]
set n4 [$ns node]
set n5 [$ns node]
set n6 [$ns node]

# ══════════════════════════════════════
# THE KEY COMMAND: make-lan
# Connects all nodes to same shared Ethernet LAN
# ══════════════════════════════════════
$ns make-lan "$n1 $n2 $n3 $n4 $n5 $n6" 0.2Mb 20ms LL Queue/DropTail Mac/802_3
#  LAN speed: 0.2 Mbps (slow to show collisions)
#  Delay: 20ms
#  LL = Link Layer
#  Mac/802_3 = Ethernet MAC (CSMA/CD)

# Two TCP flows on the shared LAN
set tcpsendagent1 [new Agent/TCP]       ;# sender on n1
set tcpsendagent2 [new Agent/TCP]       ;# sender on n2
set tcprecvagent1 [new Agent/TCPSink]   ;# receiver on n5
set tcprecvagent2 [new Agent/TCPSink]   ;# receiver on n6

$ns attach-agent $n1 $tcpsendagent1
$ns attach-agent $n2 $tcpsendagent2
$ns attach-agent $n5 $tcprecvagent1
$ns attach-agent $n6 $tcprecvagent2

# FTP applications
set app1 [new Application/FTP]
set app2 [new Application/FTP]
$app1 attach-agent $tcpsendagent1
$app2 attach-agent $tcpsendagent2

# IMPORTANT: connect AFTER creating agents
$ns connect $tcpsendagent1 $tcprecvagent1   ;# n1 ↔ n5
$ns connect $tcpsendagent2 $tcprecvagent2   ;# n2 ↔ n6

$ns at 0.1 "$app1 start"
$ns at 0.4 "$app2 start"

proc finish {} {
    global ns tr nam
    $ns flush-trace
    close $tr
    close $nam
    exec gawk -f anal.awk LAN.tr &    ;# run awk analysis script
    exit 0
}

$ns at 10 "finish"
$ns run
```

---

## 📄 Experiment 11 — Wireless LAN (wireless_lan.tcl)

### Same as Exp 5 (Wireless), but uses 802.11 LAN setup

Key configuration differences from Exp 5:

```tcl
# 15 nodes instead of 6
set val(nn) 15
# Larger area
set val(x) 800
set val(y) 800
# MAC is 802_11 (WiFi) — same as Exp 5
set val(mac) Mac/802_11

# Uses LossMonitor instead of TCPSink (to measure packet loss!)
set sink1 [new Agent/LossMonitor]   ;# ← tracks bytes AND losses
$ns attach-agent $n(1) $sink1
```

### The record {} Procedure (Calculates aggregate throughput)

```tcl
proc record {} {
    global sink1 sink3 sink7 sink10 sink11 f0
    set ns [Simulator instance]
    set time 0.5
    # Sum bytes from ALL 5 sinks
    set totalBytes [expr [$sink1 set bytes_] + [$sink3 set bytes_] + \
                         [$sink7 set bytes_] + [$sink10 set bytes_] + [$sink11 set bytes_]]
    set now [$ns now]
    puts $f0 "$now [expr $totalBytes/$time*8/1000000]"   ;# total throughput
    foreach sink {sink1 sink3 sink7 sink10 sink11} {
        $sink set bytes_ 0    ;# reset all sinks
    }
    $ns at [expr $now + $time] "record"
}
```

---

## 🆚 Ethernet LAN vs Wireless LAN

| Feature | Ethernet LAN (Exp 10) | Wireless LAN (Exp 11) |
|---------|----------------------|----------------------|
| Medium | Wired cable | Radio waves |
| MAC | `Mac/802_3` (CSMA/CD) | `Mac/802_11` (CSMA/CA) |
| NS2 command | `make-lan` | `node-config` + channels |
| Node positions | Not needed | X, Y coordinates |
| Mobility | None | `setdest` |
| Collision | Detected (CD) | Avoided (CA) |
| Scale | 6 nodes | 15 nodes |

---

## 🆚 Exp 4 (Wired) vs Exp 10 (Ethernet LAN) — Key Difference

```tcl
# Exp 4 — Point-to-point links (dedicated bandwidth):
$ns duplex-link $n0 $n1 5Mb 2ms DropTail    ;# n0 and n1 each get 5Mb

# Exp 10 — Shared LAN (all nodes compete for same bandwidth):
$ns make-lan "$n1 $n2 $n3 $n4 $n5 $n6" 0.2Mb 20ms LL Queue/DropTail Mac/802_3
#            All 6 nodes SHARE 0.2Mb total!
```

---

## 🔑 CSMA/CD vs CSMA/CA

| | CSMA/CD (Ethernet) | CSMA/CA (WiFi) |
|--|-------------------|-----------------| 
| Collision | **Detects** it after it happens | **Avoids** it before it happens |
| After collision | Stop, wait random time, retransmit | Wait for clear channel, random backoff |
| Why different? | Wired: easy to detect collision | Wireless: can't detect while transmitting |

---

## ❓ Quick Quiz

1. What NS2 command creates a shared LAN (vs point-to-point links)?
2. What MAC type is used for Ethernet? For WiFi?
3. What is the difference between CSMA/CD and CSMA/CA?
4. What does `Agent/LossMonitor` do that `Agent/TCPSink` doesn't?

<details>
<summary>✅ Answers</summary>

1. `$ns make-lan "node1 node2 ..." bandwidth delay LL Queue/DropTail Mac/802_3`
2. Ethernet = `Mac/802_3`. WiFi = `Mac/802_11`
3. CD = **Collision Detection** (Ethernet detects after collision). CA = **Collision Avoidance** (WiFi avoids by checking channel first)
4. `Agent/LossMonitor` tracks both received **bytes AND packet losses** — useful for measuring actual loss rate in wireless experiments
</details>

---

## 🏁 You Did It! — Final Summary

```
Exp 1  → TCP Socket chat (basics of networking in Java)
Exp 2  → Secure file transfer (Caesar cipher, upload/download protocol)
Exp 3  → RMI (remote procedure call over network)
Exp 9  → DHCP (UDP, automatic IP assignment)
Exp 4  → NS2 Wired network (the 5-phase NS2 recipe)
Exp 5  → NS2 Wireless (mobility, topography, AODV)
Exp 6  → Flow/Congestion control (RED vs DropTail, 5 TCP variants)
Exp 7  → DVR vs LSR (one line changes the routing protocol)
Exp 8  → Multicasting (groups, join/leave, Dense Mode)
Exp 10 → Ethernet LAN (make-lan, 802.3, shared medium)
Exp 11 → Wireless LAN (802.11, 15 nodes, LossMonitor)
```

---

## 🔗 Navigation

| ← Prev | Current | Done! |
|--------|---------|-------|
| [Exp 8: Multicasting ←](../8_Multicasting/LEARN.md) | **Exp 10 & 11: LAN** | 🎉 All experiments covered! |

[↑ Back to Master Guide](../LEARN_START_HERE.md)
