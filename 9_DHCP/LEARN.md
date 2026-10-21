# 🌐 Experiment 9 — DHCP (Dynamic Host Configuration Protocol)

> **Builds on Exp 1 (sockets) but uses UDP instead of TCP.**
> DHCP = the protocol that gives your device an IP address when you join a network.

---

## 🧠 Mental Model

```
You join a WiFi network. Your laptop has NO IP yet.
LAPTOP: "Hey anyone! I need an IP address!" (broadcast)
DHCP SERVER: "OK here you go: 192.168.1.10" (reply)
LAPTOP: Now it has an IP and can talk to the internet.
```

**UDP vs TCP — Why UDP for DHCP?**

| TCP | UDP |
|-----|-----|
| Needs connection first | No connection needed |
| `new Socket(host, port)` | `new DatagramSocket()` |
| Reliable | Fast, no overhead |
| Can't broadcast | Can broadcast (DHCP needs to broadcast!) |

> DHCP uses UDP because the device **has no IP yet** — it can't set up a TCP connection!

---

## 📄 DHCPServer.java — Line by Line

```java
import java.net.*;
import java.util.*;

public class DHCPServer {
    // Pool of IP addresses available to assign
    private static List<String> availableIpAddresses = new ArrayList<>();

    public static void main(String[] args) {
        // 🔵 STEP 1: Pre-fill IP pool (192.168.1.2 to 192.168.1.254)
        for (int i = 2; i <= 254; i++)
            availableIpAddresses.add("192.168.1." + i);

        try {
            // 🔵 STEP 2: Create UDP socket on port 4900
            DatagramSocket socket = new DatagramSocket(4900);

            while (true) {
                // 🔵 STEP 3: Wait for a client request (DHCP DISCOVER)
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);   // blocks until packet arrives

                // 🔵 STEP 4: Extract who asked and assign them an IP
                InetAddress clientAddress = receivePacket.getAddress();
                String allocatedIp = availableIpAddresses.remove(0);  // take from pool

                // 🔵 STEP 5: Send the assigned IP back (DHCP OFFER)
                byte[] responseData = ("Allocated IP: " + allocatedIp).getBytes();
                DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length,
                    clientAddress,          // send back to client
                    receivePacket.getPort() // on the same port they used
                );
                socket.send(responsePacket);

                System.out.println("Allocated IP " + allocatedIp + " to MAC 00:11:22:33:44:55");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 📄 DHCPClient.java — Line by Line

```java
import java.net.*;

public class DHCPClient {
    public static void main(String[] args) {
        // try-with-resources: socket auto-closes when done
        try (DatagramSocket socket = new DatagramSocket()) {

            // 🟢 STEP 1: Prepare request (DHCP DISCOVER)
            InetAddress serverAddress = InetAddress.getByName("127.0.0.1"); // server's IP
            byte[] requestData = ("DHCP Request with MAC: 00:11:22:33:44:55").getBytes();

            // 🟢 STEP 2: Send the request to server port 4900
            DatagramPacket requestPacket = new DatagramPacket(
                requestData, requestData.length, serverAddress, 4900
            );
            socket.send(requestPacket);

            // 🟢 STEP 3: Wait for server's response (DHCP OFFER)
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);  // blocks until reply arrives

            // 🟢 STEP 4: Print the assigned IP
            String response = new String(receivePacket.getData()).trim();
            System.out.println("Received DHCP Response: " + response);

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 🆚 TCP Sockets (Exp 1) vs UDP Sockets (Exp 9)

| Action | TCP (Exp 1) | UDP (Exp 9) |
|--------|-------------|-------------|
| Server socket | `new ServerSocket(port)` | `new DatagramSocket(port)` |
| Client socket | `new Socket(host, port)` | `new DatagramSocket()` |
| Send data | `dos.writeUTF(string)` | `socket.send(packet)` |
| Receive data | `dis.readUTF()` | `socket.receive(packet)` |
| Packet type | Stream (no wrapping needed) | `DatagramPacket` (must wrap data) |
| Connection | Required (accept/connect) | NOT required |

---

## 🔑 DatagramPacket — The Key Object

**For SENDING:**
```java
// you need: data, length, destination IP, destination port
new DatagramPacket(data, data.length, serverAddress, 4900);
```

**For RECEIVING:**
```java
// you need: empty buffer, buffer size
new DatagramPacket(receiveData, receiveData.length);
// after socket.receive(packet): packet now contains the received data
```

---

## ❓ Quick Quiz

1. Why does DHCP use UDP instead of TCP?
2. What Java class represents a UDP socket?
3. What is a `DatagramPacket`?
4. How does the server know WHERE to send the response back?

<details>
<summary>✅ Answers</summary>

1. Because the device has **no IP address yet** — it can't set up a TCP connection. Also it needs to broadcast.
2. `DatagramSocket`
3. A wrapper around raw bytes that also includes destination/source IP and port — required for UDP
4. `receivePacket.getAddress()` and `receivePacket.getPort()` give the sender's address and port
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Exp 3: RMI ←](../3_RMI/LEARN.md) | **Exp 9: DHCP** | [Exp 4: Wired NS2 →](../4_wired_network_simulation/LEARN.md) |
