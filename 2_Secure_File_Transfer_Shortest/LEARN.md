# 🔐 Experiment 2 — Secure File Transfer (TCP)

> **Core Idea:** Two Java programs transfer **file contents** over TCP — with a simple Caesar cipher to encrypt data in transit.

---

## 🧠 Mental Model Before Reading Code

```
SERVER                                    CLIENT
  |                                          |
  |  ServerSocket(7777) <- listens           |
  |       |                                  |
  |  ss.accept() <- BLOCKS                   |  new Socket("localhost", 7777)
  |       | <------- CONNECTION -------------|
  |       |                                  |
  |  reads file, encrypts ch+3         ---->  |
  |  sends "end" sentinel              ---->  |
  |                                          |  saves to ClientTXT.txt
  |                                          |
  |  waits to receive                        |  reads file, encrypts ch-3
  |  saves to ServerTXT.txt          <-----  |
  |                                  <-"end" |
```

**Think of it like a secure fax machine:**
- Server sends its file first (encrypted +3) -> Client receives & saves
- Then Client sends its file (encrypted -3) -> Server receives & saves

---

## 📄 ServerTCP.java — Line by Line

```java
import java.io.*;
import java.net.*;

class ServerTCP {
    public static void main(String args[]) throws Exception {

        // 🔵 STEP 1: Listen on port 7777
        ServerSocket ss = new ServerSocket(7777);

        // 🔵 STEP 2: Block until a client connects
        Socket s = ss.accept();

        // 🔵 STEP 3: Output stream to client
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());

        // 🔵 STEP 4: Read file path from keyboard
        BufferedReader dis1 = new BufferedReader(new InputStreamReader(System.in));
        String path = dis1.readLine();

        // 🔵 STEP 5: Read file + ENCRYPT (ch+3) -> send to client
        FileReader fr = new FileReader(path);
        int ch;
        while ((ch = fr.read()) != -1) {
            dos.writeUTF((char)(ch + 3) + "");  // Caesar shift +3
        }
        dos.writeUTF("end");  // sentinel: "file done"

        // 🔵 STEP 6: Receive encrypted file from client -> save
        FileWriter fw = new FileWriter("ServerTXT.txt");
        DataInputStream dis = new DataInputStream(s.getInputStream());
        String data;
        while (!(data = dis.readUTF()).equalsIgnoreCase("end")) {
            fw.write(data);
        }

        fw.close(); dos.close(); dis.close(); s.close(); ss.close();
    }
}
```

---

## 📄 ClientTCP.java — Line by Line

```java
import java.io.*;
import java.net.*;

class ClientTCP {
    public static void main(String args[]) throws Exception {

        // 🟢 STEP 1: Connect to server
        Socket s = new Socket("localhost", 7777);

        // 🟢 STEP 2: Receive encrypted file from server -> save
        FileWriter fw = new FileWriter("ClientTXT.txt");
        DataInputStream dis = new DataInputStream(s.getInputStream());
        String data;
        while (!(data = dis.readUTF()).equalsIgnoreCase("end")) {
            fw.write(data);
        }
        fw.close();

        // 🟢 STEP 3: Read file path from keyboard
        DataOutputStream dos = new DataOutputStream(s.getOutputStream());
        BufferedReader dis1 = new BufferedReader(new InputStreamReader(System.in));
        String path = dis1.readLine();

        // 🟢 STEP 4: Read file + ENCRYPT (ch-3) -> send to server
        FileReader fr = new FileReader(path);
        int ch;
        while ((ch = fr.read()) != -1) {
            dos.writeUTF((char)(ch - 3) + "");  // Caesar shift -3
        }
        dos.writeUTF("end");

        fr.close(); dos.close(); dis.close(); s.close();
    }
}
```

---

## 🔐 The Caesar Cipher — How Encryption Works

| Side | Direction | Formula | Example ('A' = 65) |
|------|-----------|---------|----------------------|
| Server sends | Encrypt | ch + 3 | 'A' -> 'D' |
| Client sends | Encrypt | ch - 3 | 'A' -> '>' |

Each character is shifted before being sent. Without knowing the shift value, an eavesdropper cannot read the original file.

> ⚠️ Note: In this "shortest" version, received files are stored as-is (still encrypted). A full version would decrypt on arrival.

---

## 🔁 Key Classes Used

| Class | Purpose |
|-------|---------|
| `ServerSocket(7777)` | Opens port 7777, waits for clients |
| `Socket("localhost", 7777)` | Client connects to server |
| `DataOutputStream.writeUTF(str)` | Sends a UTF-8 String over the socket |
| `DataInputStream.readUTF()` | Reads a UTF-8 String from the socket |
| `FileReader` | Reads a file character by character |
| `FileWriter` | Writes characters to a file |

> **Memory trick:** writeUTF / readUTF always work in pairs — one side writes, other reads.

---

## 🔁 Sentinel Pattern — "end" Signal

TCP is a stream — no built-in message boundaries for files. We need a "done" signal:

```java
dos.writeUTF("end");   // sender: "I am done sending the file"

while (!(data = dis.readUTF()).equalsIgnoreCase("end")) {
    fw.write(data);    // receiver: keep looping until "end" arrives
}
```

This is a classic pattern — also used in HTTP (Content-Length), FTP, etc.

---

## ▶️ How to Run

```bash
# Terminal 1 — Start the Server FIRST
javac ServerTCP.java
java ServerTCP
# Prompt: Enter the path of the File: hello.txt

# Terminal 2 — Start the Client
javac ClientTCP.java
java ClientTCP
# Prompt: Enter the path of the File: world.txt
```

After running:
- `ClientTXT.txt` — server's file received by client (encrypted)
- `ServerTXT.txt` — client's file received by server (encrypted)

---

## ❓ Quick Quiz (Test Yourself)

1. What does `dos.writeUTF("end")` signal to the receiver?
2. Which side starts sending first — Server or Client?
3. What file does the client save the received data into?
4. What would happen if you removed the "end" sentinel?

<details>
<summary>✅ Answers</summary>

1. It is a **sentinel** — tells the receiver the file transfer is complete, breaking the loop
2. **Server** sends first; Client receives first, then sends
3. **ClientTXT.txt** (created in the current working directory)
4. `readUTF()` would **block forever** — the receiver never knows the file is done

</details>

---

## 🔗 Navigation

| Prev | Current | Next |
|------|---------|------|
| [Exp 1: Socket Chat](../1_socket_chat_terminal/LEARN.md) | **Exp 2: Secure File Transfer (Shortest)** | Exp 3 coming soon |
