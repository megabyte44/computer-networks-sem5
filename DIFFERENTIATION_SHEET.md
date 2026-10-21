# 🧬 Computer Networks Lab — Master Differentiation & Diff Sheet

> **The Secret to CN Lab:** 80% of every NS2 script is the **exact same boilerplate**.  
> You only need to memorize **the standard 7-step skeleton once**, and then remember **the 2 to 4 unique lines (the "diff")** for each experiment.

---

## 🧭 Table of Contents
1. [The Universal 7-Step NS2 Skeleton (Pseudocode)](#1-the-universal-7-step-ns2-skeleton)
2. [Master Comparison Matrix (At-a-Glance)](#2-master-comparison-matrix-at-a-glance)
3. [Experiment-by-Experiment Git Diffs](#3-experiment-by-experiment-git-diffs)
   - [Exp 4: Wired Network Simulation (The Baseline)](#exp-4-wired-network-simulation--the-baseline)
   - [Exp 6A: Flow Control (Window Size & Periodic Record)](#exp-6a-flow-control)
   - [Exp 6B: Congestion Control (Tahoe vs Reno vs Vegas vs Newreno vs Sack1)](#exp-6b-congestion-control)
   - [Exp 7: Dynamic Routing — DVR & LSR (Failure & Recovery)](#exp-7-dynamic-routing--dvr-and-lsr)
   - [Exp 8: Multicast Routing (Dense Mode, Groups & Joins)](#exp-8-multicast-routing)
   - [Exp 10: Ethernet LAN (CSMA/CD via make-lan)](#exp-10-ethernet-lan-8023)
   - [Exp 5 & 11: Wireless LAN (802.11, AODV & Mobile Nodes)](#exp-5--11-wireless-lan-80211--aodv)
4. [Java Experiments Differentiation (TCP vs UDP vs RMI)](#4-java-experiments-differentiation)

---

## 1. The Universal 7-Step NS2 Skeleton

Every single NS2 simulation runs through these **7 steps** in the exact same order:

```text
[1. INIT]       Create Simulator ($ns)
       ↓
[2. TRACE]      Open trace (.tr) and nam (.nam) files -> trace-all & namtrace-all
       ↓
[3. NODES]      Create nodes ($ns node)
       ↓
[4. TOPOLOGY]   Connect nodes with links ($ns duplex-link OR make-lan OR wireless coords)
       ↓
[5. TRAFFIC]    Attach Agents (TCP/UDP) + Apps (FTP/CBR) -> Connect sender to receiver
       ↓
[6. SCHEDULE]   Schedule start/stop times ($ns at 1.0 "...") and define finish {} proc
       ↓
[7. RUN]        $ns run
```

### The Standard Baseline Code (Exp 4 Wired Template)
```tcl
# 1. Simulator
set ns [new Simulator]

# 2. Trace Files
set tf [open out.tr w]
$ns trace-all $tf
set nf [open out.nam w]
$ns namtrace-all $nf

# 3. Nodes
set n0 [$ns node]
set n1 [$ns node]
set n2 [$ns node]
set n3 [$ns node]

# 4. Topology (Links)
$ns duplex-link $n0 $n1 10Mb 10ms DropTail
$ns duplex-link $n1 $n2 10Mb 10ms DropTail
$ns duplex-link $n2 $n3 10Mb 10ms DropTail

# 5. Agents & Applications
set tcp [new Agent/TCP]
set sink [new Agent/TCPSink]
$ns attach-agent $n0 $tcp
$ns attach-agent $n3 $sink
$ns connect $tcp $sink

set ftp [new Application/FTP]
$ftp attach-agent $tcp

# 6. Schedule & Finish
$ns at 1.0 "$ftp start"
$ns at 4.0 "$ftp stop"
$ns at 5.0 "finish"

proc finish {} {
    global ns tf nf
    $ns flush-trace
    close $tf
    close $nf
    exec nam out.nam &
    exit 0
}

# 7. Run
$ns run
```

---

## 2. Master Comparison Matrix (At-a-Glance)

| Exp # | Title | Simulator Line | Links / Medium | Senders / Receivers | Secret Sauce Line(s) |
|---|---|---|---|---|---|
| **4** | **Wired Simulation** | `[new Simulator]` | `duplex-link DropTail` | TCP+FTP / UDP+CBR | *Pure baseline* |
| **6A** | **Flow Control** | `[new Simulator]` | `duplex-link DropTail` | TCP + TCPSink | `proc record {}` + `$tcp set window_ 20` |
| **6B** | **Congestion Control** | `[new Simulator]` | `duplex-link RED` | 5 TCP variants | `Agent/TCP/Reno`, `Vegas`, `Sack1`, etc. |
| **7** | **DVR & LSR** | `[new Simulator]` | `duplex-link DropTail` | UDP + CBR | `$ns rtproto DV` + `$ns rtmodel-at 2.0 down` |
| **8** | **Multicast** | `[new Simulator -multicast on]` | `duplex-link DropTail` | UDP + LossMonitor | `$ns mrtproto DM {}` + `join-group` / `leave-group` |
| **10** | **Ethernet LAN** | `[new Simulator]` | `$ns make-lan` | 2x TCP + TCPSink | `Mac/802_3` + virtual LAN node id |
| **11** | **Wireless LAN** | `[new Simulator]` | *No links* (Coordinates) | CBR + LossMonitor | `node-config`, `create-god`, `setdest` |

---

## 3. Experiment-by-Experiment Git Diffs

The sections below use GitHub-style diff blocks:
- `+` means **code you MUST add** for this experiment.
- `-` means **code from the base template that was removed or replaced**.
- Unmarked lines are the standard template.

---

### Exp 4: Wired Network Simulation — The Baseline
* **Pseudocode:**
  1. Initialize Simulator, open `.tr` and `.nam`.
  2. Create 6 nodes (`n0` to `n5`).
  3. Create point-to-point links with `duplex-link ... DropTail`.
  4. Create two flows: UDP flow (`n0 -> n3`) with CBR, and TCP flow (`n2 -> n5`) with FTP.
  5. Schedule start times, finish procedure, and run.

* **AWK Key:** Counts total packets dropped / sent / received.
```awk
if ($1 == "d") drop++;
```

---

### Exp 6A: Flow Control
* **What's different?**
  - We observe how the sliding window controls packet sending.
  - Adds a recursive `proc record {}` that polls `$sink set bytes_` every 0.1s and writes throughput to a file.
  - Sets the TCP receiver advertised window (`window_`) and sender congestion window (`maxcwnd_`).

```diff
  set ns [new Simulator]
+ set f1 [open flow_control.tr w]  ;# file for xgraph throughput plot
  ...
  set tcp [new Agent/TCP]
  set sink [new Agent/TCPSink]
+ $tcp set window_ 20              ;# Flow control advertised window limit
+ $tcp set maxcwnd_ 20             ;# Max congestion window size
  ...
+ # RECURSIVE RECORD PROCEDURE FOR XGRAPH
+ proc record {} {
+     global sink f1
+     set ns [Simulator instance]
+     set time 0.1
+     set now [$ns now]
+     set bw [$sink set bytes_]
+     puts $f1 "$now [expr $bw/$time*8/1000000]"
+     $sink set bytes_ 0
+     $ns at [expr $now+$time] "record"
+ }
+ 
+ $ns at 0.0 "record"
  $ns at 1.0 "$ftp start"
```

---

### Exp 6B: Congestion Control
* **What's different?**
  - Evaluates how different TCP variants react to congestion and packet drops.
  - The bottleneck link uses `RED` (Random Early Detection) instead of `DropTail`.
  - Uses the specific agent classes for each congestion algorithm:

```diff
- $ns duplex-link $n1 $n2 10Mb 10ms DropTail
+ $ns duplex-link $n1 $n2 1Mb 50ms RED    ;# Bottleneck queue uses RED!

- set tcp [new Agent/TCP]                 ;# Default Tahoe
+ # Change the Agent class to test different congestion variants:
+ # 1. Tahoe:       [new Agent/TCP]
+ # 2. Reno:        [new Agent/TCP/Reno]
+ # 3. Newreno:     [new Agent/TCP/Newreno]
+ # 4. Vegas:       [new Agent/TCP/Vegas]
+ # 5. Sack1:       [new Agent/TCP/Sack1]  (use with Agent/TCPSink/Sack1)
```

---

### Exp 7: Dynamic Routing — DVR and LSR
* **What's different?**
  - Tests dynamic routing recalculation after a link fails.
  - Only **3 distinct lines** are added!
  - 1 line for the protocol: `$ns rtproto DV` (Distance Vector) or `$ns rtproto LS` (Link State).
  - 2 lines for link down/up event: `$ns rtmodel-at 2.0 down ...` and `up`.

```diff
  set ns [new Simulator]
+ $ns rtproto DV   ;# For Distance Vector (Bellman-Ford)
+ # OR: $ns rtproto LS  ;# For Link State (Dijkstra)

  # ... create nodes and duplex-links ...
  # ... attach UDP and CBR ...

+ # SIMULATE LINK BREAK AND RECOVERY
+ $ns rtmodel-at 2.0 down $n1 $n4
+ $ns rtmodel-at 3.0 up   $n1 $n4

  $ns at 1.0 "$cbr start"
  $ns at 4.5 "$cbr stop"
  $ns at 5.0 "finish"
```

---

### Exp 8: Multicast Routing
* **What's different?**
  - Uses IP Multicast to send one stream of UDP packets to multiple receivers.
  - Simulator **must** enable `-multicast on`.
  - Multicast routing protocol set to `DM` (Dense Mode).
  - Receivers join and leave dynamically using `join-group` and `leave-group`.
  - Receivers use `Agent/LossMonitor` instead of `Null`.

```diff
- set ns [new Simulator]
+ set ns [new Simulator -multicast on]    ;# 1. Multicast flag enabled!

+ set mrthandle [$ns mrtproto DM {}]     ;# 2. Dense Mode multicast routing
+ set group1 [Node allocaddr]             ;# 3. Allocate multicast group IP

  # ... create nodes and links ...

+ set rcvr1 [new Agent/LossMonitor]       ;# 4. LossMonitor instead of Null
+ $ns attach-agent $n3 $rcvr1
+ $udp0 set dst_addr_ $group1             ;# 5. UDP destination is the GROUP, not a node!

+ # 6. Dynamic Join and Leave
+ $ns at 0.5 "$n3 join-group $rcvr1 $group1"
+ $ns at 1.5 "$n3 leave-group $rcvr1 $group1"
```

---

### Exp 10: Ethernet LAN (802.3)
* **What's different?**
  - **No individual point-to-point links!**
  - Replaces all `$ns duplex-link` calls with a single `$ns make-lan` command.
  - Emulates IEEE 802.3 CSMA/CD broadcast bus.

```diff
- $ns duplex-link $n0 $n1 10Mb 10ms DropTail
- $ns duplex-link $n1 $n2 10Mb 10ms DropTail
- $ns duplex-link $n2 $n3 10Mb 10ms DropTail
+ # All nodes share one multi-access bus:
+ $ns make-lan "$n0 $n1 $n2 $n3 $n4 $n5" 0.2Mb 1ms LL Queue/DropTail Mac/802_3
```

* **AWK Trick:**
  - In NS2, the LAN creates an extra virtual node (e.g. node id 6).
  - To count packets delivered by the LAN:
    `awk '$1 == "r" && $3 == 6 { total_bytes += $6 }'`

---

### Exp 5 & 11: Wireless LAN (802.11 & AODV)
* **What's different?**
  - **The biggest structural shift in the entire lab!**
  - No links (`duplex-link` and `make-lan` are NOT used).
  - Uses physical channels, radio propagation, antennas, topography grid, GOD object, and node coordinates.
  - Nodes move using `setdest <x> <y> <speed>`.

```diff
- set ns [new Simulator]
- set nf [open out.nam w]
- $ns namtrace-all $nf
+ set val(chan) Channel/WirelessChannel
+ set val(prop) Propagation/TwoRayGround
+ set val(netif) Phy/WirelessPhy
+ set val(mac)  Mac/802_11
+ set val(ifq)  Queue/DropTail/PriQueue
+ set val(ifqlen) 50
+ set val(ll)   LL
+ set val(ant)  Antenna/OmniAntenna
+ set val(rp)   AODV                      ;# Ad-hoc On-demand Distance Vector
+ set val(nn)   5                         ;# Number of nodes
+ 
+ set ns [new Simulator]
+ set tf [open trace.tr w];  $ns trace-all $tf
+ set nf [open trace.nam w]; $ns namtrace-all-wireless $nf 800 800
+ 
+ set topo [new Topography]
+ $topo load_flatgrid 800 800
+ create-god $val(nn)
+ set chan [new Channel/WirelessChannel]
+ 
+ $ns node-config -adhocRouting $val(rp) \
+                 -macType $val(mac) \
+                 -channel $chan \
+                 -propType $val(prop) \
+                 -phyType $val(netif) \
+                 -antType $val(ant) \
+                 -llType $val(ll) \
+                 -ifqType $val(ifq) \
+                 -ifqLen $val(ifqlen) \
+                 -agentTrace ON \
+                 -topoInstance $topo

- set n0 [$ns node]
- $ns duplex-link $n0 $n1 10Mb 10ms DropTail
+ # Create nodes and turn off random wandering:
+ set n(0) [$ns node]; $n(0) random-motion 0
+ set n(1) [$ns node]; $n(1) random-motion 0
+ set n(2) [$ns node]; $n(2) random-motion 0
+ set n(3) [$ns node]; $n(3) random-motion 0
+ set n(4) [$ns node]; $n(4) random-motion 0
+ 
+ # Set initial starting coordinates:
+ $n(0) set X_ 0.0; $n(0) set Y_ 0.0; $n(0) set Z_ 0.0
+ $n(1) set X_ 0.0; $n(1) set Y_ 0.0; $n(1) set Z_ 0.0
+ $n(2) set X_ 0.0; $n(2) set Y_ 0.0; $n(2) set Z_ 0.0
+ $n(3) set X_ 0.0; $n(3) set Y_ 0.0; $n(3) set Z_ 0.0
+ $n(4) set X_ 0.0; $n(4) set Y_ 0.0; $n(4) set Z_ 0.0
+ 
+ # Place or move nodes using setdest <x> <y> <speed>:
+ $ns at 0.0 "$n(0) setdest 100.0 100.0 3000.0"
+ $ns at 0.0 "$n(1) setdest 200.0 200.0 3000.0"
+ $ns at 0.0 "$n(2) setdest 300.0 200.0 3000.0"
+ $ns at 0.0 "$n(3) setdest 400.0 300.0 3000.0"
+ $ns at 0.0 "$n(4) setdest 500.0 300.0 3000.0"
```

---

## 4. Java Experiments Differentiation

### Pseudocode & Differences

```text
[Exp 1: Chat]         TCP Socket  → readLine() / writeUTF() in a while(true) loop
       ↓
[Exp 2: File + Cipher]TCP Socket  → read bytes from FileInputStream → Shift (c + 3) → send over socket
       ↓
[Exp 3: RMI]          No Sockets! → Interface (Remote) + Impl (UnicastRemoteObject) + rebind / lookup
       ↓
[Exp 9: DHCP]         UDP Socket  → DatagramSocket(4900) + DatagramPacket → IP pool list.remove(0)
```

### Git Diff Comparison of Networking Models

#### TCP Sockets (Exp 1 Chat & Exp 2 Secure File)
```java
// Server: Listen and accept
ServerSocket ss = new ServerSocket(5000);
Socket s = ss.accept();
DataInputStream dis = new DataInputStream(s.getInputStream());
DataOutputStream dos = new DataOutputStream(s.getOutputStream());

// Client: Connect directly to IP and Port
Socket s = new Socket("localhost", 5000);
DataOutputStream dos = new DataOutputStream(s.getOutputStream());
DataInputStream dis = new DataInputStream(s.getInputStream());
```

#### Diff to UDP Socket (Exp 9 DHCP)
```diff
- ServerSocket ss = new ServerSocket(5000);
- Socket s = ss.accept();
+ // UDP uses DatagramSocket (Connectionless - no accept, no connect)
+ DatagramSocket socket = new DatagramSocket(4900);
+ byte[] buf = new byte[1024];
+ DatagramPacket packet = new DatagramPacket(buf, buf.length);
+ socket.receive(packet);   // blocks until packet arrives
+ 
+ // Reply goes directly to packet's IP and port:
+ DatagramPacket reply = new DatagramPacket(respData, respData.length, packet.getAddress(), packet.getPort());
+ socket.send(reply);
```

#### Diff to RMI (Exp 3)
```diff
- Socket s = new Socket(...);  // No sockets at all!
+ // 1. Interface
+ public interface MyServerIntf extends Remote {
+     public String calculate(int a, int b) throws RemoteException;
+ }
+ 
+ // 2. Server binds object in registry
+ Naming.rebind("RMServer", new MyServerImpl());
+ 
+ // 3. Client calls remote method like a local method!
+ MyServerIntf obj = (MyServerIntf) Naming.lookup("rmi://localhost/RMServer");
+ String res = obj.calculate(10, 20);
```

---

## 5. Exam Checklist: How to Write Any Experiment from Scratch

When sitting in the lab exam, follow this mental workflow:
1. **Is it Java or NS2?**
   - If Java:
     - Chat? $\rightarrow$ TCP `ServerSocket` + `Socket`.
     - File + Cipher? $\rightarrow$ TCP + `(b + 3) % 256`.
     - RMI? $\rightarrow$ 4 files (`Intf`, `Impl`, `Server`, `Client`).
     - DHCP? $\rightarrow$ UDP `DatagramSocket(4900)` + `remove(0)` from IP list.
   - If NS2:
     - Start by writing the **7-step skeleton** on paper.
     - Check the **Secret Sauce Matrix**:
       - Is it Wireless? $\rightarrow$ Swap links for `val(...)`, `node-config`, `setdest`.
       - Is it LAN? $\rightarrow$ Swap links for `make-lan`.
       - Is it Multicast? $\rightarrow$ Add `-multicast on`, `mrtproto DM`, `join-group`.
       - Is it DV / LS? $\rightarrow$ Add `rtproto DV` + `rtmodel-at ... down / up`.
       - Is it Flow Control? $\rightarrow$ Add `proc record` + `set window_`.
       - Is it Congestion Control? $\rightarrow$ Change `Agent/TCP` to `Agent/TCP/Reno` etc. and use `RED`.
