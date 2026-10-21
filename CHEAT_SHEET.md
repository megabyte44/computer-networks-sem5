# ⚡ Computer Networks — FINAL CHEAT SHEET
> Last-minute revision. Read this 30 min before your exam/lab.

---

## 📌 JAVA EXPERIMENTS — Key Code Patterns

### TCP Socket (Exp 1, 2)
```java
// SERVER side
ServerSocket ss = new ServerSocket(PORT);   // listen
Socket s = ss.accept();                     // wait for client
DataInputStream  dis = new DataInputStream(s.getInputStream());  // read FROM client
DataOutputStream dos = new DataOutputStream(s.getOutputStream()); // write TO client
String msg = dis.readUTF();   // read
dos.writeUTF("reply");        // send

// CLIENT side
Socket s = new Socket("localhost", PORT);   // connect
DataOutputStream dos = new DataOutputStream(s.getOutputStream()); // TO server
DataInputStream  dis = new DataInputStream(s.getInputStream());   // FROM server
dos.writeUTF("hello");        // send
String reply = dis.readUTF(); // receive
```

### UDP Socket (Exp 9 — DHCP)
```java
// SERVER
DatagramSocket socket = new DatagramSocket(PORT);
DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
socket.receive(packet);   // wait for UDP packet
// send reply back to sender
DatagramPacket reply = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
socket.send(reply);

// CLIENT
DatagramSocket socket = new DatagramSocket();  // no port needed
InetAddress server = InetAddress.getByName("127.0.0.1");
DatagramPacket req = new DatagramPacket(data, data.length, server, SERVER_PORT);
socket.send(req);
DatagramPacket resp = new DatagramPacket(buffer, buffer.length);
socket.receive(resp);    // receive response
```

### RMI (Exp 3) — 4-file recipe
```
MyServerIntf.java → interface extends Remote { method throws RemoteException }
MyServerImpl.java → class extends UnicastRemoteObject implements MyServerIntf { ... }
MyServer.java     → Naming.rebind("RMServer", new MyServerImpl())
MyClient.java     → MyServerIntf obj = (MyServerIntf)Naming.lookup("rmi://localhost/RMServer")
```

### Caesar Cipher (Exp 2)
```java
Encrypt: out.write((c + 3) % 256);          // each byte shifted +3
Decrypt: c = (c - 3) % 256; if(c<0) c+=256; // shifted back -3
```

---

## 📌 NS2 EXPERIMENTS — 5-Phase Template

```tcl
# PHASE 1: Simulator
set ns [new Simulator]
$ns rtproto DV          ;# only for Exp 7 (DV or LS)

# PHASE 2: Trace files
set tf [open out.tr w];  $ns trace-all $tf
set nf [open out.nam w]; $ns namtrace-all $nf        ;# wired
                         $ns namtrace-all-wireless $nf $X $Y ;# wireless only

# PHASE 3: Topology
set n0 [$ns node]
$ns duplex-link $n0 $n1 BANDWIDTH DELAY QUEUE
# Queue: DropTail | RED
# OR for LAN:
$ns make-lan "$n1 $n2 $n3" BANDWIDTH DELAY LL Queue/DropTail Mac/802_3

# PHASE 4: Agents + Applications
set tcp  [new Agent/TCP]         set sink [new Agent/TCPSink]
set udp  [new Agent/UDP]         set null [new Agent/Null]
$ns attach-agent $n0 $tcp;       $ns attach-agent $n3 $sink
$ns connect $tcp $sink
set ftp [new Application/FTP];   $ftp attach-agent $tcp
set cbr [new Application/Traffic/CBR]; $cbr attach-agent $udp

# PHASE 5: Events + Run
$ns at 1.0 "$ftp start"
$ns at 5.0 "finish"
proc finish {} { global ns tf nf; $ns flush-trace; close $tf; close $nf; exit 0 }
$ns run
```

---

## 📌 WIRELESS EXTRA STEPS (Exp 5, 11)

```tcl
# BEFORE creating nodes:
set val(chan) Channel/WirelessChannel
set val(prop) Propagation/TwoRayGround
set val(netif) Phy/WirelessPhy
set val(mac)  Mac/802_11
set val(ifq)  Queue/DropTail/PriQueue
set val(ll)   LL
set val(ant)  Antenna/OmniAntenna
set val(rp)   AODV
set val(x) 500; set val(y) 500; set val(nn) 6

set topo [new Topography]
$topo load_flatgrid $val(x) $val(y)
create-god $val(nn)
set chan [new $val(chan)]

$ns node-config -adhocRouting $val(rp) -llType $val(ll) \
  -macType $val(mac) -ifqType $val(ifq) -ifqLen 50 \
  -antType $val(ant) -propType $val(prop) -phyType $val(netif) \
  -topoInstance $topo -agentTrace ON -macTrace ON \
  -routerTrace ON -movementTrace ON -channel $chan

# AFTER creating nodes:
$n0 set X_ 10.0; $n0 set Y_ 20.0; $n0 set Z_ 0.0  ;# set position
$ns at 1.0 "$n1 setdest 490.0 340.0 25.0"          ;# set movement
```

---

## 📌 MULTICAST EXTRA STEPS (Exp 8)

```tcl
set ns [new Simulator -multicast on]   ;# MUST have -multicast on
set mproto DM
set mrthandle [$ns mrtproto $mproto {}]
set group1 [Node allocaddr]            ;# create group address
# Sender: $udp0 set dst_addr_ $group1
# Receiver: $ns at 1.0 "$n5 join-group $rcvr1 $group1"
# Leave:    $ns at 4.0 "$n5 leave-group $rcvr1 $group1"
```

---

## 📌 DVR vs LSR (Exp 7)

```tcl
$ns rtproto DV   ;# Distance Vector (Bellman-Ford, slow convergence)
$ns rtproto LS   ;# Link State (Dijkstra, fast convergence)
$ns rtmodel-at 2.0 down $nodeA $nodeB  ;# simulate link failure
$ns rtmodel-at 3.0 up   $nodeA $nodeB  ;# restore link
```

---

## 📌 CONGESTION CONTROL VARIANTS (Exp 6)

```tcl
Agent/TCP/Vegas    ;# proactive (delays before loss)
Agent/TCP/Reno     ;# Fast Retransmit + Fast Recovery
Agent/TCP/Newreno  ;# better multi-loss handling
Agent/TCP/Sack1    ;# Selective ACK (most efficient)
Agent/TCP          ;# Tahoe (basic: slow start)
```

---

## 📌 RECORD PROCEDURE (all NS2 exps)

```tcl
proc record {} {
    global sink ft
    set ns [Simulator instance]
    set time 0.1
    set now [$ns now]
    set bw [$sink set bytes_]
    puts $ft "$now [expr $bw/$time*8/1000000]"   ;# throughput (Mbps)
    $sink set bytes_ 0
    $ns at [expr $now+$time] "record"
}
$ns at 0.5 "record"   ;# kick off recording
```

---

## 📌 QUICK COMPARISONS

| | TCP | UDP |
|--|-----|-----|
| Java class | `Socket` + `ServerSocket` | `DatagramSocket` |
| Reliable? | Yes | No |
| Ordered? | Yes | No |
| Connection? | Required | Not needed |
| Experiments | Exp 1, 2, 3 | Exp 9 |

| | DVR | LSR |
|--|-----|-----|
| NS2 | `rtproto DV` | `rtproto LS` |
| Algorithm | Bellman-Ford | Dijkstra |
| Knows | Neighbor costs | Full network map |
| Speed | Slow | Fast |

| | Ethernet (Exp 10) | WiFi (Exp 11) |
|--|-------------------|---------------|
| MAC | `Mac/802_3` | `Mac/802_11` |
| Command | `make-lan` | `node-config` |
| Collision | CSMA/**CD** | CSMA/**CA** |

[↑ Back to Master Guide](LEARN_START_HERE.md)
