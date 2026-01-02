# Architecture

## Overview
The system follows a **client–server architecture** over TCP sockets.
- A central server listens for incoming client connections.
- Each client connection is handled by a dedicated thread.
- Clients communicate exclusively through the server.

## Core Components

### Server
- Accepts incoming connections
- Assigns unique client IDs
- Maintains the coordinator role
- Routes broadcast and private messages
- Handles client disconnections and coordinator reassignment

### Client
- Connects to the server via TCP
- Sends and receives messages
- Displays coordinator status dynamically
- Provides a GUI-based interface for interaction

### Coordinator
- A designated client
- Responsible for group-level coordination
- Periodically distributes client status information


