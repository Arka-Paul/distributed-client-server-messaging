package server;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler extends Thread {
    private Socket socket;
    private Server server;
    private PrintWriter out;
    private Scanner in;
    private int clientId;
    private boolean isCoordinator = false;
    private Thread coordinatorThread;
    private volatile boolean running = true;

    // Constructor to initialize the client handler with socket, ID, and server reference
    public ClientHandler(Socket socket, int clientId, Server server) {
        this.socket = socket;
        this.clientId = clientId;
        this.server = server;

        try {
            out = new PrintWriter(socket.getOutputStream(), true); // Used to send messages
            in = new Scanner(socket.getInputStream());             // Used to receive messages
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getClientId() {
        return clientId;
    }

    public int getClientPort() {
        return socket.getPort(); // Get the port of the connected client
    }

    // Assign or remove coordinator status to this client
    public void setCoordinator(boolean isCoordinator) {
        if (this.isCoordinator && !isCoordinator) {
            stopCoordinatorThread(); // Stop the coordinator task if being demoted
        }

        this.isCoordinator = isCoordinator;

        if (isCoordinator) {
            sendMessage("You are now the coordinator.");
            server.broadcastMessage("Server: Client " + clientId + " is now the coordinator.", -1);
            startCoordinatorThread();
        }
    }

    // Coordinator thread sends active clients list every 20 seconds
    private void startCoordinatorThread() {
        coordinatorThread = new Thread(() -> {
            try {
                while (running && isCoordinator && !socket.isClosed()) {
                    Thread.sleep(20000); // Wait for 20 seconds

                    StringBuilder list = new StringBuilder("Active Clients:\n\n");
                    server.getClients().keySet().stream().sorted().forEach(id -> {
                        ClientHandler ch = server.getClients().get(id);
                        if (ch != null) {
                            String ip = server.getFakeClientIP(id);
                            int port = ch.getClientPort();
                            boolean isCoord = id == server.getCoordinatorId();

                            list.append("Client ").append(id);
                            if (isCoord) list.append(" (Coordinator)");
                            list.append(" [ID: ").append(id)
                                .append("] [IP Address: ").append(ip)
                                .append("] [Port: ").append(port).append("]\n");
                        }
                    });

                    sendMessage(list.toString());
                }
            } catch (InterruptedException ignored) {}
        });
        coordinatorThread.start();
    }

    // Stop the coordinator thread if running
    private void stopCoordinatorThread() {
        isCoordinator = false;
        if (coordinatorThread != null && coordinatorThread.isAlive()) {
            coordinatorThread.interrupt();
            coordinatorThread = null;
        }
    }

    // Sends a message to the client
    public void sendMessage(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    // Main execution for the thread - handles incoming messages
    @Override
    public void run() {
        try {
            sendMessage("Welcome! Your ID is " + clientId);

            // Send coordinator-related message depending on the role
            if (isCoordinator) {
                sendMessage("You are the coordinator.");
                startCoordinatorThread();
            } else {
                int coordId = server.getCoordinatorId();
                ClientHandler ch = server.getClients().get(coordId);
                if (ch != null) {
                    sendMessage("Current Coordinator: Client " + coordId +
                            " [IP Address: " + server.getFakeClientIP(coordId) + "]");
                }
            }

            // Continuously listen for input from client
            while (in.hasNextLine()) {
                String message = in.nextLine().trim();

                // If the coordinator requests to see all members
                if (message.equalsIgnoreCase("!members") && isCoordinator) {
                    StringBuilder list = new StringBuilder("Active Clients:\n");
                    server.getClients().keySet().stream().sorted().forEach(id -> {
                        ClientHandler ch = server.getClients().get(id);
                        list.append("- Client ").append(ch.getClientId());
                        if (id == server.getCoordinatorId()) list.append(" (Coordinator)");
                        list.append(" [ID: ").append(ch.getClientId())
                            .append("] [IP Address: ").append(server.getFakeClientIP(id))
                            .append("] [Port: ").append(ch.getClientPort()).append("]\n");
                    });
                    sendMessage(list.toString());
                }

                // Handle group member info request
                else if (message.equalsIgnoreCase("!requestinfo")) {
                    if (server.getCoordinatorId() == clientId) {
                        sendMessage("You are the coordinator. You already have the info.");
                    } else {
                        ClientHandler coordinator = server.getClients().get(server.getCoordinatorId());
                        if (coordinator != null) {
                            coordinator.sendInfoRequestPopup(clientId);
                        } else {
                            sendMessage("Coordinator not found.");
                        }
                    }
                }

                // Private message handling (starts with @)
                else if (message.startsWith("@")) {
                    String[] parts = message.split(" ", 2);
                    if (parts.length == 2) {
                        int targetClientId = Integer.parseInt(parts[0].substring(1));
                        server.sendPrivateMessage(targetClientId, "Private from " + clientId + ": " + parts[1]);
                    }
                }

                // General message to all clients
                else {
                    server.broadcastMessage("Client " + clientId + ": " + message, clientId);
                }
            }
        } catch (Exception e) {
            System.out.println("Client " + clientId + " disconnected.");
        } finally {
            running = false;
            stopCoordinatorThread();
            server.removeClient(clientId);
        }
    }

    // Show popup for the coordinator to approve or deny info sharing
    public void sendInfoRequestPopup(int requesterId) {
        SwingUtilities.invokeLater(() -> {
            int result = JOptionPane.showConfirmDialog(null,
                    "Client " + requesterId + " requested group member info.\nDo you want to allow?",
                    "Info Request", JOptionPane.YES_NO_OPTION);

            ClientHandler requester = server.getClients().get(requesterId);

            if (result == JOptionPane.YES_OPTION) {
                if (requester != null) {
                    StringBuilder info = new StringBuilder("Group Member Details:\n\n");
                    for (ClientHandler ch : server.getClients().values()) {
                        int id = ch.getClientId();
                        String fakeIP = server.getFakeClientIP(id);
                        int port = ch.getClientPort();
                        boolean isCoord = id == server.getCoordinatorId();

                        info.append("Client ").append(id);
                        if (isCoord) info.append(" (Coordinator)");
                        info.append(" [ID: ").append(id)
                            .append("] [IP Address: ").append(fakeIP)
                            .append("] [Port: ").append(port).append("]\n");
                    }
                    requester.sendMessage(info.toString());
                }
            } else {
                if (requester != null) {
                    requester.sendMessage("Your request for group info was denied.");
                }
            }
        });
    }
}
