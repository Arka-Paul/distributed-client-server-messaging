package client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket socket;              // Socket connection to the server
    private PrintWriter out;           // Used for sending messages to the server
    private Scanner in;                // Used for receiving messages from the server

    // Tries to establish a connection to the server using the provided IP and port
    public boolean connect(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);                 // Open socket connection
            out = new PrintWriter(socket.getOutputStream(), true);   // Enable message output
            in = new Scanner(socket.getInputStream());               // Enable message input
            return true;
        } catch (Exception e) {
            return false; // Connection failed
        }
    }

    // Sends a message to the server if connection is active
    public void sendMessage(String message) {
        if (out != null && isConnected()) {
            out.println(message);  // Send the message to the server
        }
    }

    // Reads an incoming message from the server
    public String readMessage() {
        try {
            if (in != null && in.hasNextLine()) {
                return in.nextLine();  // Return the next message from server
            }
        } catch (Exception ignored) {}
        return null; // No message received
    }

    // Disconnects the client from the server gracefully
    public void disconnect() {
        try {
            if (out != null && !socket.isClosed()) {
                out.println("quit");  // Notify the server that the client is disconnecting
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();      // Close the socket connection
            }
        } catch (Exception e) {
            System.out.println("Error during disconnect: " + e.getMessage());
        } finally {
            out = null;
            in = null;
            socket = null; // Reset all resources
        }
    }

    // Checks whether the client is still connected to the server
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
