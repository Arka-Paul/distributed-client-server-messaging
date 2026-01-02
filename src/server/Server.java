package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    // Main server socket to accept client connections
    protected ServerSocket serverSocket;

    // Stores all active clients using their ID
    protected Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();

    // Fake IP mapping for each client
    protected Map<Integer, String> fakeIPs = new ConcurrentHashMap<>();

    // Generates unique client IDs
    protected AtomicInteger clientIdGenerator = new AtomicInteger(1);

    // ID of the current coordinator client
    protected int coordinatorId = -1;

    // Used to generate random IP suffixes
    private final Random rand = new Random();

    // Starts the server and listens for incoming client connections
    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            // Continuously accept new client connections
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientIdGenerator.getAndIncrement();

                // Create and store a new handler for the connected client
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId, this);
                addClient(clientHandler);

                // If no coordinator exists yet, assign this client as coordinator
                if (coordinatorId == -1) {
                    coordinatorId = clientId;
                    clientHandler.setCoordinator(true);
                    System.out.println("Client " + clientId + " is now the coordinator.");
                }

                // Start the thread for handling client communication
                clientHandler.start();
            }
        } catch (IOException e) {
            System.out.println("Server socket closed.");
        }
    }

    // Sends a message to all clients except the sender
    public synchronized void broadcastMessage(String message, int senderId) {
        for (ClientHandler client : clients.values()) {
            if (client.getClientId() != senderId) {
                client.sendMessage(message);
            }
        }
    }

    // Sends a private message to a specific client
    public synchronized void sendPrivateMessage(int targetClientId, String message) {
        ClientHandler targetClient = clients.get(targetClientId);
        if (targetClient != null) {
            targetClient.sendMessage(message);
        }
    }

    // Removes a client and handles coordinator reassignment if needed
    public synchronized void removeClient(int clientId) {
        clients.remove(clientId);
        fakeIPs.remove(clientId);

        // If the removed client was coordinator, reassign a new one
        if (clientId == coordinatorId) {
            if (!clients.isEmpty()) {
                int newCoordinatorId = clients.keySet().iterator().next();
                coordinatorId = newCoordinatorId;
                ClientHandler newCoordinator = clients.get(newCoordinatorId);

                if (newCoordinator != null) {
                    newCoordinator.setCoordinator(true);

                    // Notify all clients about the new coordinator
                    String info = "Client " + newCoordinatorId + " [IP Address: " + getFakeClientIP(newCoordinatorId) + "]";
                    for (ClientHandler ch : clients.values()) {
                        ch.sendMessage("[COORDINATOR_CHANGED] " + info);
                    }
                }
            } else {
                // No clients left, reset coordinator
                coordinatorId = -1;
            }
        }

        updateClientList();
    }

    // Adds a new client to the server's list
    public synchronized void addClient(ClientHandler clientHandler) {
        clients.put(clientHandler.getClientId(), clientHandler);
        updateClientList();
    }

    // Returns the full map of active clients
    public Map<Integer, ClientHandler> getClients() {
        return clients;
    }

    // Gracefully shuts down the server and disconnects all clients
    public synchronized void stop() {
        try {
            // Inform all clients before shutting down
            for (ClientHandler client : clients.values()) {
                client.sendMessage("Server is shutting down.");
            }

            // Clear internal mappings and close the server socket
            clients.clear();
            fakeIPs.clear();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            System.out.println("Server stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hook for GUI to update client list display (overridden in subclasses)
    public synchronized void updateClientList() {
        // GUI will override this method
    }

    // Returns the ID of the current coordinator
    public int getCoordinatorId() {
        return coordinatorId;
    }

    // Generates and returns a fake IP address for the client
    public String getFakeClientIP(int clientId) {
        return fakeIPs.computeIfAbsent(clientId, id -> "192.168.1." + (rand.nextInt(100) + 1));
    }
}
