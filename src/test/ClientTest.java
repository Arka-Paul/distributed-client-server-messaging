package test;

import static org.junit.jupiter.api.Assertions.*;

import client.Client;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;

class ClientTest {

    private ServerSocket serverSocket;
    private Client client;

    @BeforeEach
    void setUp() throws IOException {
        serverSocket = new ServerSocket(0); // Bind to any free port
        client = new Client();
    }

    @AfterEach
    void tearDown() throws IOException {
        client.disconnect();
        if (!serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    @Test
    void testConnect() throws Exception {
        int port = serverSocket.getLocalPort();

        new Thread(() -> {
            try {
                serverSocket.accept().close();
            } catch (IOException ignored) {}
        }).start();

        boolean result = client.connect("localhost", port);
        assertTrue(result);
        assertTrue(client.isConnected());
    }

    @Test
    void testSendMessage() throws Exception {
        int port = serverSocket.getLocalPort();
        StringBuilder received = new StringBuilder();

        new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                received.append(reader.readLine());
            } catch (IOException ignored) {}
        }).start();

        client.connect("localhost", port);
        client.sendMessage("Hello Test");
        Thread.sleep(100); // Let message arrive

        assertEquals("Hello Test", received.toString());
    }

    @Test
    void testReadMessage() throws Exception {
        int port = serverSocket.getLocalPort();

        new Thread(() -> {
            try (Socket socket = serverSocket.accept()) {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println("Hello Client");
            } catch (IOException ignored) {}
        }).start();

        client.connect("localhost", port);
        String message = client.readMessage();
        assertEquals("Hello Client", message);
    }

    @Test
    void testDisconnect() throws Exception {
        int port = serverSocket.getLocalPort();

        new Thread(() -> {
            try (Socket socket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String quit = in.readLine();
                assertEquals("quit", quit);
            } catch (IOException ignored) {}
        }).start();

        client.connect("localhost", port);
        client.disconnect();
        assertFalse(client.isConnected());
    }
    
    @Test
    void testIsConnected() throws Exception {
        int port = serverSocket.getLocalPort();

        new Thread(() -> {
            try {
                serverSocket.accept();
            } catch (IOException ignored) {}
        }).start();

        boolean connected = client.connect("localhost", port);
        assertTrue(connected);
        assertTrue(client.isConnected());
    }
}
