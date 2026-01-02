package test;

import static org.junit.jupiter.api.Assertions.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import server.Server;
import server.ClientHandler;

class ServerTest {

    private Server server;

    @BeforeEach
    void setUp() {
        server = new Server();
    }

    @Test
    void testAddClient() {
        DummyClientHandler handler = new DummyClientHandler(1);
        server.addClient(handler);

        assertEquals(1, server.getClients().size());
        assertTrue(server.getClients().containsKey(1));
    }

    @Test
    void testRemoveClient() {
        DummyClientHandler handler = new DummyClientHandler(2);
        server.addClient(handler);
        server.removeClient(2);

        assertFalse(server.getClients().containsKey(2));
    }

    @Test
    void testGetClients() {
        DummyClientHandler handler1 = new DummyClientHandler(1);
        DummyClientHandler handler2 = new DummyClientHandler(2);
        server.addClient(handler1);
        server.addClient(handler2);

        assertEquals(2, server.getClients().size());
    }

    @Test
    void testBroadcastMessage() {
        DummyClientHandler sender = new DummyClientHandler(1);
        DummyClientHandler receiver1 = new DummyClientHandler(2);
        DummyClientHandler receiver2 = new DummyClientHandler(3);

        server.addClient(sender);
        server.addClient(receiver1);
        server.addClient(receiver2);

        server.broadcastMessage("Hello everyone!", sender.getClientId());

        assertEquals(List.of("Hello everyone!"), receiver1.getMessages());
        assertEquals(List.of("Hello everyone!"), receiver2.getMessages());
        assertTrue(sender.getMessages().isEmpty());
    }

    @Test
    void testSendPrivateMessage() {
        DummyClientHandler handler = new DummyClientHandler(4);
        server.addClient(handler);

        server.sendPrivateMessage(4, "Private msg!");
        assertEquals(List.of("Private msg!"), handler.getMessages());
    }

    @Test
    void testStop() {
        DummyClientHandler handler = new DummyClientHandler(5);
        server.addClient(handler);

        server.stop();
        assertTrue(server.getClients().isEmpty());
        assertEquals(List.of("Server is shutting down."), handler.getMessages());
    }

    @Test
    void testGetCoordinatorId() {
        assertEquals(-1, server.getCoordinatorId()); // Default before any client is added
    }

    @Test
    void testGetFakeClientIP() {
        String ip = server.getFakeClientIP(6);
        assertNotNull(ip);
        assertTrue(ip.startsWith("192.168.1."));
    }

    // Optional: testing GUI override placeholder
    @Test
    void testUpdateClientList() {
        // Since updateClientList is empty (overridden by GUI subclasses), we just check it doesn’t crash
        server.updateClientList();
        assertTrue(true);
    }

    // Dummy ClientHandler subclass
    static class DummyClientHandler extends ClientHandler {
        private final int id;
        private final List<String> messages = new ArrayList<>();

        public DummyClientHandler(int id) {
            super(createFakeSocket(), id, new Server());
            this.id = id;
        }

        @Override
        public int getClientId() {
            return id;
        }

        @Override
        public void sendMessage(String message) {
            messages.add(message);
        }

        @Override
        public void setCoordinator(boolean isCoordinator) {
            // Do nothing
        }

        public List<String> getMessages() {
            return messages;
        }

        private static Socket createFakeSocket() {
            try {
                ServerSocket serverSocket = new ServerSocket(0); // OS assigns a free port
                int port = serverSocket.getLocalPort();

                new Thread(() -> {
                    try (Socket ignored = serverSocket.accept()) {
                        // Accept and immediately discard the incoming connection
                        serverSocket.close(); // ✅ Properly close the ServerSocket
                    } catch (Exception ignored) {
                    }
                }).start();

                return new Socket("localhost", port);
            } catch (Exception e) {
                return null;
            }
        }

    }
}
