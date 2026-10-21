# 🚦 Experiment 6 — Flow Control & Congestion Control (NS2)

> **Same NS2 structure as Exp 4/5, but now we STUDY how TCP manages traffic speed.**
> Two separate simulations: one for **flow control**, one for **congestion control**.

---

## 🧠 Mental Model

```
FLOW CONTROL: "Don't overwhelm the RECEIVER"
  TCP sender asks: "How much buffer space do you have left?"
  Receiver says: "I have X bytes free (Window Size)"
  Sender limits how much it sends to that X

CONGESTION CONTROL: "Don't overwhelm the NETWORK"
  TCP detects packet loss = congestion
  TCP slows down (reduces congestion window)
  TCP speeds up again slowly (AIMD: Additive Increase, Multiplicative Decrease)
```

---

## 🌊 Part A — Flow Control (flow_control.tcl)

### Topology
```
n(0) ──1Mb/DropTail── n(1) ──1Mb/RED── n(2)
[TCP sender]                           [TCP sink]
```

> **Notice:** The n(1)→n(2) link uses **RED** queue type instead of DropTail!

### Key Code

```tcl
set ns [new Simulator]

# Output files for plotting
set ft [open "Throughput" "w"]    ;# stores throughput data
set fb [open "Bandwidth"  "w"]    ;# stores bandwidth data

# 3 nodes in a line
for {set i 0} {$i < 3} {incr i} {
    set n($i) [$ns node]
}

# Links — RED on second link (active queue management)
$ns duplex-link $n(0) $n(1) 1Mb 10ms DropTail
$ns duplex-link $n(1) $n(2) 1Mb 10ms RED       ;# RED = congestion-aware queue

# TCP sender at n(0), TCPSink at n(2)
set tcp  [new Agent/TCP]
set null [new Agent/TCPSink]
$ns attach-agent $n(0) $tcp
$ns attach-agent $n(2) $null
$ns connect $tcp $null

# HTTP Exponential traffic (bursty, like web browsing)
set http [new Application/Traffic/Exponential]
$http attach-agent $tcp

$ns at 0.2 "$http start"
$ns at 3.2 "$http stop"

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# RECORD PROCEDURE — measures throughput every 0.1 seconds
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
proc record {} {
    global ft fb null
    set ns [Simulator instance]
    set time 0.1                         ;# measurement interval
    set now [$ns now]                    ;# current simulation time

    set bw [$null set bytes_]            ;# bytes received since last check

    # Throughput (Mbps) = bytes * 8 bits / time / 1,000,000
    puts $ft "$now [expr $bw/$time*8/1000000]"
    puts $fb "$now [expr $bw]"

    $null set bytes_ 0                   ;# reset counter for next interval

    $ns at [expr $now+$time] "record"    ;# schedule next measurement
}

$ns at 0.5 "record"    ;# start recording at t=0.5s
$ns at 5.0 "finish"
$ns run
```

---

## 🔥 Part B — Congestion Control (congestion_control.tcl)

### What's Different?
This experiment compares **5 different TCP congestion control algorithms** running simultaneously!

| Variable | TCP Variant | Behavior |
|----------|------------|----------|
| `tcp1` | `Agent/TCP/Vegas` | Estimates congestion before packet loss |
| `tcp2` | `Agent/TCP/Reno` | Fast Retransmit + Fast Recovery |
| `tcp3` | `Agent/TCP/Newreno` | Improvement on Reno |
| `tcp4` | `Agent/TCP/Sack1` | Selective ACK — retransmit only lost packets |
| `tcp5` | `Agent/TCP` | TCP Tahoe (basic: slow start + congestion avoidance) |

### How They React to Congestion

```
TCP Tahoe:   CWND drops to 1 on packet loss (harsh)
TCP Reno:    CWND halves on packet loss (better)
TCP NewReno: Handles multiple packet losses better than Reno
TCP SACK:    Can retransmit exactly lost segments (most efficient)
TCP Vegas:   Proactively slows down before loss (smoothest)
```

### Key Code Structure

```tcl
# 6 nodes in a custom topology
for {set i 0} {$i < 6} {incr i} { set n($i) [$ns node] }

# Links — some use RED (congestion-aware)
$ns duplex-link $n(0) $n(1) 10Kb  10ms DropTail
$ns duplex-link $n(0) $n(3) 100Kb 10ms RED        ;# ← RED causes early drops
$ns duplex-link $n(1) $n(2) 50Kb  10ms DropTail
$ns duplex-link $n(2) $n(5) 200Kb 10ms RED
...

# 5 different TCP senders, all to n(5)
set tcp1 [new Agent/TCP/Vegas]
set tcp2 [new Agent/TCP/Reno]
set tcp3 [new Agent/TCP/Newreno]
set tcp4 [new Agent/TCP/Sack1]
set tcp5 [new Agent/TCP]        ;# = Tahoe

# Each has its own HTTP Exponential traffic
set http1 [new Application/Traffic/Exponential]
$http1 attach-agent $tcp1
```

---

## 🔑 DropTail vs RED

| Queue Type | When packet arrives & buffer is full |
|------------|--------------------------------------|
| `DropTail` | Drop the packet. Simple. |
| `RED` | Randomly drop packets BEFORE buffer is full. Signals congestion early! |

> **RED = Random Early Detection** — prevents global sync (all TCPs slowing at same time)

---

## 📊 The record {} Procedure Pattern (Memorize This!)

This pattern appears in ALL NS2 experiments that measure performance:

```tcl
proc record {} {
    global sink ft              ;# access global variables
    set ns [Simulator instance] ;# get the running simulator
    set time 0.1                ;# measurement interval in seconds
    set now [$ns now]           ;# current time
    set bw [$sink set bytes_]   ;# bytes received in last interval

    # Calculate throughput in Mbps and write to file
    puts $ft "$now [expr $bw/$time*8/1000000]"

    $sink set bytes_ 0          ;# RESET the counter!
    $ns at [expr $now+$time] "record"   ;# schedule next measurement
}
```

---

## ❓ Quick Quiz

1. What is the difference between flow control and congestion control?
2. What does RED queue type do differently from DropTail?
3. Which TCP variant uses Selective ACK? What's the advantage?
4. In the `record {}` proc, why do we set `bytes_ 0` at the end?

<details>
<summary>✅ Answers</summary>

1. **Flow control** prevents overwhelming the **receiver** (receiver advertises window size). **Congestion control** prevents overwhelming the **network** (TCP reacts to packet loss)
2. RED drops packets **randomly before the buffer is full** to give early warning. DropTail waits until completely full
3. **TCP SACK** (Sack1). It can tell the sender exactly which packets were lost, so only those are retransmitted — not everything after the gap
4. We reset so the **next** measurement only counts NEW bytes — otherwise it would accumulate and give wrong readings
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Exp 5: Wireless NS2 ←](../5_wireless_network_simulation/LEARN.md) | **Exp 6: Flow & Congestion** | [Exp 7: DVR & LSR →](../7_DVR_and_LSR/LEARN.md) |
