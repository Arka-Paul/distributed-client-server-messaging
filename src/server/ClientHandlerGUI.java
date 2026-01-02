package server;

import client.ClientGUI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ClientHandlerGUI {

    private JFrame frame;
    private JTextField portField;
    private JLabel statusLabel;
    private JTextArea logArea;
    private JList<String> clientList;
    private DefaultListModel<String> clientListModel;
    private JTextField broadcastField;
    private JPanel clientPanelContainer;
    private CardLayout cardLayout;

    private Server server;
    private Thread serverThread;

    private int clientCount = 0;
    private Map<String, JPanel> clientPanels = new HashMap<>();

    public static void main(String[] args) {
        // Launch GUI on the Event Dispatch Thread
        EventQueue.invokeLater(() -> {
            try {
                ClientHandlerGUI window = new ClientHandlerGUI();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Constructor initialises the GUI
    public ClientHandlerGUI() {
        initialize();
    }

    // Setup GUI components and listeners
    private void initialize() {
        frame = new JFrame("Chat Server GUI (ClientHandlerGUI)");
        frame.setBounds(100, 100, 800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null);

        // Port input and control buttons
        JLabel lblPort = new JLabel("Port:");
        lblPort.setBounds(20, 20, 50, 20);
        frame.getContentPane().add(lblPort);

        portField = new JTextField("6666");
        portField.setBounds(60, 20, 100, 25);
        frame.getContentPane().add(portField);

        JButton btnStart = new JButton("Start Server");
        btnStart.setBounds(180, 20, 130, 25);
        frame.getContentPane().add(btnStart);

        JButton btnAddClient = new JButton("Add Client");
        btnAddClient.setBounds(320, 20, 120, 25);
        frame.getContentPane().add(btnAddClient);

        JButton btnStop = new JButton("Stop Server");
        btnStop.setBounds(450, 20, 130, 25);
        frame.getContentPane().add(btnStop);

        // Label to show server status
        statusLabel = new JLabel("Status: Not running");
        statusLabel.setBounds(20, 60, 300, 20);
        frame.getContentPane().add(statusLabel);

        // Text area for server logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBounds(20, 90, 430, 300);
        frame.getContentPane().add(scrollPane);

        // List to show added clients
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setBounds(470, 90, 300, 200);
        frame.getContentPane().add(clientScrollPane);

        // Broadcast message input
        JLabel lblBroadcast = new JLabel("Broadcast:");
        lblBroadcast.setBounds(20, 410, 100, 20);
        frame.getContentPane().add(lblBroadcast);

        broadcastField = new JTextField();
        broadcastField.setBounds(100, 410, 300, 25);
        frame.getContentPane().add(broadcastField);

        JButton btnSend = new JButton("Send");
        btnSend.setBounds(410, 410, 80, 25);
        frame.getContentPane().add(btnSend);

        // Panel for embedded client windows
        clientPanelContainer = new JPanel();
        clientPanelContainer.setBounds(470, 300, 300, 150);
        clientPanelContainer.setLayout(new CardLayout());
        frame.getContentPane().add(clientPanelContainer);

        cardLayout = (CardLayout) clientPanelContainer.getLayout();

        // Start Server button logic
        btnStart.addActionListener(e -> {
            int port = Integer.parseInt(portField.getText());

            // Custom server instance with GUI-aware logging
            server = new Server() {
                @Override
                public synchronized void broadcastMessage(String message, int senderId) {
                    super.broadcastMessage(message, senderId);
                    log("Broadcast: " + message);
                }

                @Override
                public synchronized void sendPrivateMessage(int targetClientId, String message) {
                    super.sendPrivateMessage(targetClientId, message);
                    log("Private to " + targetClientId + ": " + message);
                }

                @Override
                public synchronized void removeClient(int clientId) {
                    super.removeClient(clientId);
                    updateClientList();
                    log("Client " + clientId + " disconnected.");
                }

                @Override
                public synchronized void updateClientList() {
                    clientListModel.clear();
                    for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
                        int id = entry.getKey();
                        boolean isCoord = id == coordinatorId;
                        clientListModel.addElement("Client " + id + (isCoord ? " (Coordinator)" : ""));
                    }
                }

                @Override
                public synchronized void addClient(ClientHandler handler) {
                    clients.put(handler.getClientId(), handler);
                    updateClientList();
                }
            };

            // Start the server on a new thread
            serverThread = new Thread(() -> server.start(port));
            serverThread.start();
            statusLabel.setText("Server running on port " + port);
            log("Server started on port " + port);
        });

        // Add Client button logic
        btnAddClient.addActionListener(e -> {
            String clientName = "Client " + (++clientCount);
            ClientGUI clientGui = new ClientGUI();
            JPanel panel = clientGui.createClientPanel(clientName, "Client 1");

            clientPanels.put(clientName, panel);
            clientPanelContainer.add(panel, clientName);
            clientListModel.addElement(clientName);
            clientList.setSelectedValue(clientName, true);
            cardLayout.show(clientPanelContainer, clientName);
        });

        // Handle selection change in client list
        clientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = clientList.getSelectedValue();
                if (selected != null && clientPanels.containsKey(selected)) {
                    cardLayout.show(clientPanelContainer, selected);
                }
            }
        });

        // Send broadcast message from input field
        btnSend.addActionListener(e -> {
            String msg = broadcastField.getText().trim();
            if (!msg.isEmpty() && server != null) {
                server.broadcastMessage("Server: " + msg, -1);
                broadcastField.setText("");
            }
        });
    }

    // Add log messages to the log area
    private void log(String message) {
        logArea.append(message + "\n");
    }
}
