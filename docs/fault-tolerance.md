# Fault Tolerance (Coordinator Failover)

## Coordinator Model
At any given time, one connected client is designated as the **coordinator**.  
This role is used for group-level operations and coordination tasks.

## Failover Behaviour
If the coordinator disconnects:
1. The server removes the disconnected client
2. The next available client is promoted to coordinator
3. All connected clients are notified of the change

This ensures continued system operation without requiring a server restart.
