# 📞 Experiment 3 — RMI (Remote Method Invocation)

> **Builds on Exp 1 & 2:** Instead of sending raw bytes or strings,
> you **call a method on a remote machine** as if it were local.

---

## 🧠 Mental Model

```
Exp 1: Client sends string → Server reads string → sends reply string
Exp 2: Client sends bytes  → Server saves file
Exp 3: Client calls add(2000, 500) on the SERVER → gets back 2500
        └── It looks like a local function call, but runs on the server!
```

**RMI = Remote Method Invocation**
> "I'm calling a function, but the function runs on ANOTHER computer."

```
CLIENT                              SERVER (RMI Registry)
  │                                      │
  │  Naming.lookup("rmi://localhost/RMServer")
  │◄──── gets a "stub" (proxy object) ───│
  │                                      │
  │  asif.add(2000, 500)                 │
  │──────── RMI sends call ─────────────►│ MyServerImpl.add(2000,500) executes
  │◄──────── returns 2500 ──────────────-│
```

---

## 📁 4 Files You MUST Know

| File | Role | 1-Line Summary |
|------|------|----------------|
| `MyServerIntf.java` | Interface | Declares what remote methods exist |
| `MyServerImpl.java` | Implementation | Actually runs the method |
| `MyServer.java` | Server main | Registers the object with RMI Registry |
| `MyClient.java` | Client main | Looks up and calls the remote method |

---

## 📄 File 1: MyServerIntf.java — The Contract

```java
import java.rmi.*;

// This interface tells the CLIENT what methods are available on the server
public interface MyServerIntf extends Remote {        // must extend Remote
    int add(int a, int b) throws RemoteException;     // every method must throw RemoteException
}
```

> **Why extend `Remote`?** It marks this interface as callable from another machine.
> **Why `throws RemoteException`?** Network calls can fail — we must declare that.

---

## 📄 File 2: MyServerImpl.java — The Actual Work

```java
import java.rmi.*;
import java.rmi.server.*;

// UnicastRemoteObject = supports TCP-based point-to-point RMI
public class MyServerImpl extends UnicastRemoteObject implements MyServerIntf {
    MyServerImpl() throws RemoteException {}  // constructor must declare this

    public int add(int a, int b) throws RemoteException {
        return (a + b);  // this runs ON THE SERVER when client calls it
    }
}
```

---

## 📄 File 3: MyServer.java — Register with RMI

```java
import java.rmi.*;

public class MyServer {
    public static void main(String[] arg) {
        try {
            MyServerImpl myserver = new MyServerImpl();  // create the object
            Naming.rebind("RMServer", myserver);          // register it with name "RMServer"
            System.out.println("Server Started...");
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}
```

> `Naming.rebind("RMServer", myserver)` = "Tell the RMI Registry: if anyone asks for 'RMServer', give them this object"

---

## 📄 File 4: MyClient.java — Call the Remote Method

```java
import java.rmi.*;

public class MyClient {
    public static void main(String[] arg) {
        try {
            String sName = "rmi://localhost/RMServer";  // address of the remote object

            // Look up the server object — returns a stub (proxy)
            MyServerIntf asif = (MyServerIntf) Naming.lookup(sName);

            int d1 = 2000, d2 = 500;
            System.out.println("Addition: " + asif.add(d1, d2));  // calls method on server!
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}
```

---

## ▶️ How to Run (Order Matters!)

```bash
# Step 1: Compile everything
javac MyServerIntf.java MyServerImpl.java MyServer.java MyClient.java

# Step 2: Generate stub (older Java)
rmic MyServerImpl

# Step 3: Start RMI Registry (in background)
start rmiregistry

# Step 4: Start the server
java MyServer

# Step 5: Run the client (in new terminal)
java MyClient
# Output: Addition: 2500
```

---

## 🆚 Exp 1 vs Exp 3 — What Changed?

| Feature | Exp 1 (Socket Chat) | Exp 3 (RMI) |
|---------|---------------------|-------------|
| Communication | Raw bytes/strings | Method calls |
| What client does | `dos.writeUTF("hello")` | `asif.add(2000, 500)` |
| Feels like | Sending a message | Calling a local function |
| Registry | None | RMI Registry (`rmiregistry`) |
| Interface | None | `extends Remote` |

---

## ❓ Quick Quiz

1. What is the purpose of `MyServerIntf`?
2. Why must every RMI method `throws RemoteException`?
3. What does `Naming.rebind("RMServer", myserver)` do?
4. What does `Naming.lookup("rmi://localhost/RMServer")` return?

<details>
<summary>✅ Answers</summary>

1. It's the **contract** (interface) that tells the client which methods are available on the server
2. Network calls can fail at any time — Java forces you to handle this possibility
3. Registers the server object with the RMI Registry under the name "RMServer"
4. A **stub** (proxy object) that the client uses to call remote methods as if they were local
</details>

---

## 🔗 Navigation

| ← Prev | Current | Next → |
|--------|---------|--------|
| [Exp 2: Secure File Transfer ←](../2_Secure_File_Transfer/LEARN.md) | **Exp 3: RMI** | [Exp 9: DHCP →](../9_DHCP/LEARN.md) |
