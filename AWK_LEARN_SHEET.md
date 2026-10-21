# 📊 AWK Scripts Master Learning & Differentiation Sheet

> **The Secret to AWK in CN Lab:** In this entire lab, there are really only **TWO AWK templates**.  
> Every experiment only changes the **node filter condition** and prints **specific values (a, b, c, d)** in the `END` block.

---

## 🧭 Table of Contents
1. [The 3-Block AWK Mental Model](#1-the-3-block-awk-mental-model)
2. [Trace File Column Decoders (Wired vs Wireless)](#2-trace-file-column-decoders)
3. [Experiment-by-Experiment Cheat Table (Check & Display)](#3-experiment-by-experiment-cheat-table)
4. [Master Template 1: Wired Trace (Exp 6A, 6B, 7)](#4-master-template-1-wired-trace-exp-6a-6b-7)
5. [Master Template 2: Wired Packet & Drop Counter (Exp 4)](#5-master-template-2-wired-packet--drop-counter-exp-4)
6. [Master Template 3: Ethernet LAN $3 == 6 Rule (Exp 10)](#6-master-template-3-ethernet-lan-3--6-rule-exp-10)
7. [Master Template 4: Wireless Trace & PDR (Exp 5 & 11)](#7-master-template-4-wireless-trace--pdr-exp-5--11)
8. [Formulas & Math Cheatsheet](#8-formulas--math-cheatsheet)

---

## 1. The 3-Block AWK Mental Model

Every AWK script is structured into three parts:

```awk
BEGIN {
    # Part 1: Runs ONCE before reading the file.
    # Initialize your counters and variables to 0.
}
{
    # Part 2: Runs on EVERY SINGLE LINE of the trace file.
    # Filter the line with if (...) and count or sum bytes.
}
END {
    # Part 3: Runs ONCE after reaching end of the file.
    # Perform math (delay, throughput, delivery ratio) and print results.
}
```

---

## 2. Trace File Column Decoders

### A. Wired Trace Columns (`wired.tr`, `out.tr`, `LAN.tr`)
Example line: `r 1.0456 2 3 tcp 1040 ------- 1 2.0 3.0 0 0`

| Column | Meaning | What to Look For |
|---|---|---|
| `$1` | **Event** | `r` (received), `+` (enqueued), `-` (dequeued), `d` (dropped) |
| `$2` | **Time** | Timestamp of event (used for start time `st` and end time `ft`) |
| `$3` | **From Node** | Source or previous hop node ID |
| `$4` | **To Node** | Destination or next hop node ID (e.g. `7`, `5`) |
| `$5` | **Packet Type** | `tcp`, `cbr`, `ack` |
| `$6` | **Packet Size** | Size in bytes (e.g. `1040`, `500` — used to sum data) |

---

### B. Wireless Trace Columns (`trace.tr`, `complex.tr`)
Example line: `s 20.000000000 _0_ AGT --- 0 cbr 1000 ...`

| Column | Meaning | What to Look For |
|---|---|---|
| `$1` | **Event** | `s` (send), `r` (receive), `D` or `d` (drop) |
| `$2` | **Time** | Timestamp of event |
| `$3` | **Node ID** | e.g. `_0_` |
| `$4` | **Layer** | `AGT` (Application Agent), `RTR` (Routing), `MAC` (MAC layer) |
| `$6` | **Packet ID / Seq** | Packet sequence number |
| `$7` | **Packet Type** | `cbr`, `tcp` |
| `$8` | **Packet Size** | Bytes |

---

## 3. Experiment-by-Experiment Cheat Table

| Exp # | Trace File Type | What Condition to Filter In `{ ... }` | What to Display in `END { ... }` |
|---|---|---|---|
| **Exp 4 (Wired)** | Wired | Drop: `$1 == "d"`<br>Receive: `$1 == "r" && $5 == "tcp"` | **a)** Number of Packets Received (`received`)<br>**b)** Number of Packets Dropped (`dropped`)<br>**c)** Throughput (`(received * 8) / (stop - start)`) |
| **Exp 6A (Flow Control)** | Wired | Received at node 7:<br>`$1 == "r" && $4 == 7` | **a)** Start time (`st1`)<br>**b)** End time (`ft1`)<br>**c)** Total data bytes (`data1 += $6`)<br>**d)** Delay (`ft1 - st1`) & Throughput (`data1 / delay1`) |
| **Exp 6B (Congestion Control)** | Wired | Received at node 5:<br>`$1 == "r" && $4 == 5` | **a)** Start time (`st1`)<br>**b)** End time (`ft1`)<br>**c)** Total data bytes (`data1 += $6`)<br>**d)** Delay (`ft1 - st1`) & Throughput (`data1 / delay1`) |
| **Exp 7 (DVR & LSR)** | Wired | Received at node 7:<br>`$1 == "r" && $4 == 7` | **a)** Start time (`st1`)<br>**b)** End time (`ft1`)<br>**c)** Total data bytes (`data1 += $6`)<br>**d)** Delay (`ft1 - st1`) & Throughput (`data1 / delay1`) |
| **Exp 10 (Ethernet LAN)** | Wired (LAN) | Received from LAN virtual node 6:<br>`$1 == "r" && $3 == 6` | **a)** Total filesize bytes (`filesize1 += $6`)<br>**b)** Latency (`endtime1 - starttime1`)<br>**c)** Throughput in Mbps (`(filesize1 / latency1) / 1e6`) |
| **Exp 5 & 11 (Wireless LAN)** | Wireless | Sent: `$4 == "AGT" && $1 == "s"`<br>Recv: `$4 == "AGT" && $1 == "r"`<br>Drop: `$1 == "D" \|\| $1 == "d"` | **a)** Generated / Sent Packets (`sent`)<br>**b)** Received Packets (`recvd`)<br>**c)** Packet Delivery Ratio % (`(recvd / sent) * 100`)<br>**d)** Total Dropped Packets (`dropped`) |

---

## 4. Master Template 1: Wired Trace (Exp 6A, 6B, 7)

> **Used for:** Exp 6A (dest = 7), Exp 6B (dest = 5), Exp 7 (dest = 7).  
> **Rule:** Only change the node number in `$4 == ...`!

```awk
BEGIN {
    st1 = 0
    ft1 = 0
    data1 = 0
    flag1 = 0
}
{
    # Check receive event ($1=="r") at the destination node ($4==...)
    if ($1 == "r" && $4 == 7) {
        data1 += $6                  # add packet size in bytes

        if (flag1 == 0) {
            st1 = $2                 # record first packet arrival time
            flag1 = 1
        }
        ft1 = $2                     # constantly update with latest time
    }
}
END {
    delay1 = ft1 - st1
    throughput1 = data1 / delay1

    print "********** HTTP / TCP **********"
    print "Start Time : ", st1
    print "End Time   : ", ft1
    print "Total Data : ", data1, "bytes"
    print "Delay      : ", delay1, "sec"
    print "Throughput : ", throughput1, "bytes/sec"
}
```

---

## 5. Master Template 2: Wired Packet & Drop Counter (Exp 4)

> **Used for:** Exp 4 Wired Network Simulation.  
> **Displays:** Packets Received, Packets Dropped, and Throughput in bits/sec.

```awk
BEGIN {
    received = 0
    dropped = 0
    start = 1.0
    stop = 3.0
}
{
    # Count TCP received packets
    if ($1 == "r" && $5 == "tcp") {
        received++
    }

    # Count dropped packets
    if ($1 == "d") {
        dropped++
    }
}
END {
    print "Number of Packets Received = ", received
    print "Number of Packets Dropped  = ", dropped
    print "Throughput                 = ", (received * 8) / (stop - start), "bits per second"
}
```

---

## 6. Master Template 3: Ethernet LAN $3 == 6 Rule (Exp 10)

> **Used for:** Exp 10 Ethernet LAN (CSMA/CD 802.3).  
> **Key Trick:** All nodes connect to a LAN which NS2 creates as virtual node `6`.  
> We measure all packets delivered **from** the LAN: `$3 == 6`.

```awk
BEGIN {
    filesize1 = 0
    starttime1 = 0
    endtime1 = 0
    flag1 = 0
}
{
    # $1 == "r" (received) and $3 == 6 (coming from LAN node 6)
    if ($1 == "r" && $3 == 6) {
        if (flag1 == 0) {
            starttime1 = $2
            flag1 = 1
        }
        filesize1 += $6
        endtime1 = $2
    }
}
END {
    latency1 = endtime1 - starttime1

    if (latency1 > 0) {
        bandwidth1 = filesize1 / latency1
    }

    print "Final Values:"
    print "Filesize   : ", filesize1, "bytes"
    print "Latency    : ", latency1, "sec"
    print "Throughput : ", bandwidth1 / 1e6, "Mbps"
}
```

---

## 7. Master Template 4: Wireless Trace & PDR (Exp 5 & 11)

> **Used for:** Exp 5 and Exp 11 Wireless LAN (AODV / 802.11).  
> **Key Trick:** We only count packets at the Application Layer (`$4 == "AGT"`).

```awk
BEGIN {
    sent = 0
    recvd = 0
    dropped = 0
}
{
    # Packets sent at Application Agent layer
    if ($4 == "AGT" && $1 == "s") {
        sent++
    }

    # Packets received at Application Agent layer
    if ($4 == "AGT" && $1 == "r") {
        recvd++
    }

    # Packets dropped anywhere
    if ($1 == "D" || $1 == "d") {
        dropped++
    }
}
END {
    pdr = (sent > 0) ? (recvd / sent) * 100 : 0

    print "Packets Sent          : ", sent
    print "Packets Received      : ", recvd
    print "Packet Delivery Ratio : ", pdr, "%"
    print "Total Dropped Packets : ", dropped
}
```

---

## 8. Formulas & Math Cheatsheet

1. **Delay (Latency):**
   $$\text{Delay} = \text{End Time} - \text{Start Time} = \text{ft} - \text{st}$$

2. **Throughput (Bytes per second):**
   $$\text{Throughput} = \frac{\text{Total Bytes}}{\text{Delay}} = \frac{\text{data}}{\text{ft} - \text{st}}$$

3. **Throughput (Bits per second - bps):**
   $$\text{Throughput (bps)} = \frac{\text{Total Bytes} \times 8}{\text{Delay}}$$

4. **Throughput (Megabits per second - Mbps):**
   $$\text{Throughput (Mbps)} = \frac{\text{Throughput (bps)}}{10^6} \quad \text{or} \quad \frac{\text{Bytes} / \text{Delay}}{10^6}$$

5. **Packet Delivery Ratio (PDR %):**
   $$\text{PDR} = \left(\frac{\text{Packets Received}}{\text{Packets Sent}}\right) \times 100$$

---

## 🚀 How to Run Any AWK Script from Terminal

```bash
awk -f script.awk tracefile.tr
```
*(On some systems, use `gawk -f script.awk tracefile.tr`)*
