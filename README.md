# Distributed Client–Server Messaging System (Java)

## Overview
A Java **socket-based client–server messaging system** supporting multiple concurrent clients, **broadcast and private messaging**, and a **coordinator role** with automatic reassignment on disconnect (basic failover).

The project demonstrates **distributed systems and backend engineering fundamentals**, including networking, concurrency, coordination, fault tolerance, GUI-based interaction, and automated testing.


---

## Key Features
- TCP client–server communication using Java sockets
- Thread-per-client concurrency model
- Coordinator role with automatic reassignment on failure
- Broadcast and private messaging support
- Swing-based GUI for server and clients
- Periodic coordinator-driven client status updates
- Automated testing using JUnit 5

---

## Project Structure
- `src/client` — client networking logic and GUI
- `src/server` — server logic, coordinator handling, GUIs
- `src/test` — JUnit test cases
- `docs/` — architecture, fault tolerance, and testing notes
- `screenshots/` — optional GUI screenshots

---

## How to Run

### Option 1: Run via GUI (Recommended)
1. Run the server GUI:
   - `server.ServerGUI` (or `server.ClientHandlerGUI` if using the alternative GUI)
2. Add clients using the GUI
3. Connect clients to:
   - IP: `127.0.0.1`
   - Port: `6666` (or chosen port)

### Option 2: Run Tests via Maven
```bash
mvn clean test
```
---
## Fault Tolerance / Coordinator Failover

- The first connected client becomes the coordinator
- If the coordinator disconnects, the server automatically assigns a new coordinator
- All clients are notified when the coordinator changes

See `docs/fault-tolerance.md` for details.

---

## Testing

JUnit tests validate:
- Client connection, messaging, and disconnect lifecycle
- Server broadcast and private messaging logic
- Coordinator defaults and helper behaviour

Tests are located in `src/test`.

---

## Professional Context

This project demonstrates:
- Distributed systems fundamentals
- Backend networking and concurrency
- Fault-tolerant coordination logic
- Engineering discipline with testing and documentation

---

## Author

**Arka Paul**  
Cyber Security Graduate  
GitHub: https://github.com/Arka-Paul
