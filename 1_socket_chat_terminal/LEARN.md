# 📡 Experiment 1 — Socket Chat (TCP)

> **Core Idea:** Two Java programs talk to each other over a network using TCP Sockets.

---

## 🧠 Mental Model Before Reading Code

```
SERVER                              CLIENT
  │                                   │
  │  ServerSocket(7777) ← listens     │
  │         │                         │
  │    ss.accept() ← BLOCKS here      │  new Socket("localhost", 7777)
  │         │ ←─── CONNECTION ────────│
  │         │                         │
  │  [reads from client]              │  [types a message]
  │  [sends reply]                    │  [reads server reply]
  │         │                         │
  └─ loop until "over" ──────────────┘
```

**Think of it like a walkie-talkie:**
- Server presses listen → waits
- Client connects → conversation starts
- One talks, other listens. Back and forth. Say "over" to end.

---

## 📄 Server.java — Line by Line

```java
import java.net.*;   // Socket, ServerSocket
import java.io.*;    // DataInputStream, DataOutputStream

public class Server {
    public static void main(String args[]) throws Exception {

        // 🔵 STEP 1: Create a listening socket on port 7777
        ServerSocket ss = new ServerSocket(7777);
        System.out.println("Server Started");
        System.out.println("Waiting for Connection.....");

        // 🔵 STEP 2: BLOCK here until a client connects
        Socket s = ss.accept();  // returns only when client connects
        System.out.println("Connection established with Client...");

        // 🔵 STEP 3: Set up streams
        DataInputStream dat = new DataInputStream(System.in);  // keyboard input
        DataInputStream dis = new DataInputStream(s.getInputStream());   // from client
        DataOutputStream dos = new DataOutputStream(s.getOutputStream()); // to client

        String clientMessage = " ";
        String my_reply = " ";

        // 🔵 STEP 4: Chat loop
        while (!(clientMessage.equalsIgnoreCase("over"))) {
            clientMessage = dis.readUTF();           // read from client
            System.out.println("Client:" + clientMessage);
            System.out.println("Message:");
            my_reply = dat.readLine();               // type your reply
            dos.writeUTF(my_reply);                  // send reply to client
        }
    }
}
```

---

## 📄 Client.java — Line by Line

```java
import java.net.*;
import java.io.*;

public class Client {
    public static void main(String args[]) throws Exception {

        // 🟢 STEP 1: Connect to server at localhost:7777
        Socket s = new Socket("localhost", 7777);

        // 🟢 STEP 2: Set up streams
        DataInputStream dis = new DataInputStream(System.in);   // keyboard
        DataOutputStream dos = new DataOutputStream(s.getOutputStream()); // to server
        DataInputStream d = new DataInputStream(s.getInputStream());      // from server

        String l = " ";
        String re = " ";

        // 🟢 STEP 3: Chat loop
        while (!(l.equalsIgnoreCase("over"))) {
            System.out.println("Message:");
            l = dis.readLine();    // type message
            dos.writeUTF(l);       // send to server
            re = d.readUTF();      // read server reply
            System.out.println("Server:" + re);
        }

        // 🟢 STEP 4: Close
        s.close();
        dos.close();
        d.close();
    }
}
```

---

## 🔁 Streams — The Key Concept

| Stream | Direction | Used for |
|--------|-----------|----------|
| `DataInputStream(s.getInputStream())` | Server ← Client | Reading what the other sent |
| `DataOutputStream(s.getOutputStream())` | Server → Client | Sending data |
| `DataInputStream(System.in)` | Keyboard → Program | Reading your keyboard input |

> **Memory trick:** `getInputStream` = what comes **IN** to me. `getOutputStream` = what goes **OUT** from me.

---

## ▶️ How to Run

```bash
# Terminal 1 — Start the Server FIRST
javac Server.java
java Server

# Terminal 2 — Start the Client
javac Client.java
java Client
```

---

## ❓ Quick Quiz (Test Yourself)

1. What does `ss.accept()` do? _(Hint: it blocks)_
2. Which side creates `ServerSocket`? _(Server or Client?)_
3. If you want to read what the **client** sends, which stream do you use on the server?
4. To end the chat, what word does the client type?

<details>
<summary>✅ Answers</summary>

1. It **blocks/waits** until a client connects, then returns a `Socket`
2. **Server** creates `ServerSocket`; Client just creates `Socket`
3. `DataInputStream dis = new DataInputStream(s.getInputStream())`
4. **"over"**
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Start Here ←](../LEARN_START_HERE.md) | **Exp 1: Socket Chat** | [Exp 2: Secure File Transfer →](../2_Secure_File_Transfer/LEARN.md) |
