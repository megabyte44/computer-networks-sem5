# 🚀 Computer Networks — 10-Hour Crash Course

> You have **10 hours**. This guide connects all 11 experiments in a logical story.
> Every concept builds on the last. Follow the path, don't skip.

---

## 🗺️ THE LEARNING PATH

```
LAYER 7 (Application)
    │
    ▼
[Exp 1] Socket Chat ──────────────────── "How do two programs talk?"
    │  ↓ (add security)
[Exp 2] Secure File Transfer ─────────── "How to send files safely?"
    │  ↓ (make it remote)
[Exp 3] RMI ──────────────────────────── "How to call code on another machine?"
    │
LAYER 4 (Transport — TCP/UDP)
    │
    ▼
[Exp 9] DHCP ─────────────────────────── "How devices get IP addresses? (UDP)"
[Exp 6] Flow & Congestion Control ─────── "How does TCP manage speed?"
    │
LAYER 3 (Network — Routing)
    │
    ▼
[Exp 4] Wired Simulation ─────────────── "Simulate a wired network in NS2"
[Exp 5] Wireless Simulation ──────────── "Add wireless nodes + mobility"
[Exp 7] DVR & LSR ────────────────────── "How routers find paths?"
[Exp 8] Multicasting ─────────────────── "One sender → many receivers"
    │
LAYER 2 (Data Link)
    │
    ▼
[Exp 10] Ethernet LAN ────────────────── "Wired shared medium (802.3)"
[Exp 11] Wireless LAN ────────────────── "Wireless shared medium (802.11)"
```

---

## ⏱️ TIME BUDGET (10 Hours)

| Time      | Experiment                          | File                                               |
|-----------|-------------------------------------|----------------------------------------------------|
| Hour 1    | Exp 1 — Socket Chat                 | `1_socket_chat_terminal/LEARN.md`                  |
| Hour 2    | Exp 2 — Secure File Transfer        | `2_Secure_File_Transfer/LEARN.md`                  |
| Hour 3    | Exp 3 — RMI                         | `3_RMI/LEARN.md`                                   |
| Hour 4    | Exp 9 — DHCP (UDP concepts)         | `9_DHCP/LEARN.md`                                  |
| Hour 5    | Exp 4 — Wired NS2 Simulation        | `4_wired_network_simulation/LEARN.md`              |
| Hour 6    | Exp 5 — Wireless NS2 Simulation     | `5_wireless_network_simulation/LEARN.md`           |
| Hour 7    | Exp 6 — Flow & Congestion Control   | `6_flowcontrol_congestioncontrol/LEARN.md`         |
| Hour 8    | Exp 7 — DVR & LSR                   | `7_DVR_and_LSR/LEARN.md`                           |
| Hour 9    | Exp 8 — Multicasting                | `8_Multicasting/LEARN.md`                          |
| Hour 10   | Exp 10 & 11 — Ethernet & WiFi LAN   | `10_EthernetLAN/LEARN.md` + `11_WirelessLAN/LEARN.md` |

---

## 🧠 The Big Mental Model

### Java Experiments (Exp 1, 2, 3, 9)
```
Your program
    │
    ├── TCP Socket  → reliable, ordered, stream  (like a phone call)
    └── UDP Socket  → fast, unreliable, packets  (like a postcard)
```

### NS2 Experiments (Exp 4–8, 10–11)
```
Every NS2 script follows this recipe:
  STEP 1: set ns [new Simulator]         ← create simulator engine
  STEP 2: open .tr and .nam files        ← logging for analysis
  STEP 3: create nodes + duplex-link     ← build the topology
  STEP 4: attach agents + applications   ← put traffic on it
  STEP 5: $ns at X "event"  + $ns run   ← schedule & run
```

---

## 🔑 Key Terms Cheat Sheet

| Term | What it means |
|------|---------------|
| `Socket` | Endpoint of a network connection |
| `ServerSocket` | Listens for incoming TCP connections (server side) |
| `DatagramSocket` | UDP socket — no connection needed |
| `TCP` | Reliable, ordered byte stream (Exp 1, 2, 3) |
| `UDP` | Fast, unreliable datagrams (Exp 9) |
| `RMI` | Remote Method Invocation — call a method on another machine |
| `DHCP` | Protocol that gives IP addresses to devices automatically |
| `DVR` | Distance Vector Routing — share neighbor tables |
| `LSR` | Link State Routing — each router has full network map |
| `Multicast` | One source sends to a GROUP of receivers |
| `DropTail` | Drop packet when buffer is full |
| `RED` | Random Early Detection — drop packets to signal congestion |
| `CBR` | Constant Bit Rate traffic (UDP, like a video stream) |
| `FTP (NS2)` | App that generates steady TCP traffic in NS2 |
| `AODV` | Ad-hoc On-Demand Distance Vector — wireless routing |
| `802_3` | Ethernet standard |
| `802_11` | WiFi standard |
| `NAM` | Network Animator — visualizes NS2 simulation |
| `xgraph` | Tool to plot NS2 throughput/bandwidth data |

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| — | **START HERE** | [Exp 1: Socket Chat →](1_socket_chat_terminal/LEARN.md) |
