package client;

import javax.swing.*;
import java.awt.*;

public class ClientGUI {
    private Client client;                        // Core logic object to handle connection
    private JTextArea textArea;                   // Displays messages in the GUI
    private JTextField txtMessage;                // Input field for typing messages
    private JButton btnSend, btnConnect, btnDisconnect, btnRequestInfo;
    private JLabel lblCoordinator;                // Displays coordinator info

    // Builds and returns a JPanel containing the full client GUI
    public JPanel createClientPanel(String clientName, String initialCoordinatorName) {
        client = new Client(); // Create a new instance of the backend client

        JPanel panel = new JPanel(new BorderLayout()); // Main container panel

        // Top section containing client label and connection panel
        JPanel topPanel = new JPanel(new GridLayout(2, 1));

        // Displays client name and coordinator status
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblClient = new JLabel("You are: " + clientName);
        lblCoordinator = new JLabel("Coordinator: Not connected");
        lblCoordinator.setForeground(Color.BLUE);
        labelPanel.add(lblClient);
        labelPanel.add(lblCoordinator);

        // Contains IP and port fields with connect/disconnect buttons
        JPanel connectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtIP = new JTextField("127.0.0.1", 10);
        JTextField txtPort = new JTextField("6666", 5);
        btnConnect = new JButton("Connect");
        btnDisconnect = new JButton("Disconnect");
        btnRequestInfo = new JButton("Request Info");

        // Disable controls until connected
        btnDisconnect.setEnabled(false);
        btnSend = new JButton("Send");
        btnSend.setEnabled(false);
        btnRequestInfo.setEnabled(false);

        connectPanel.add(new JLabel("Server IP:"));
        connectPanel.add(txtIP);
        connectPanel.add(new JLabel("Port:"));
        connectPanel.add(txtPort);
        connectPanel.add(btnConnect);
        connectPanel.add(btnDisconnect);
        connectPanel.add(btnRequestInfo);

        topPanel.add(labelPanel);
        topPanel.add(connectPanel);
        panel.add(topPanel, BorderLayout.NORTH);

        // Middle section: text area for messages
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Bottom section: message input + send button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        txtMessage = new JTextField();
        bottomPanel.add(txtMessage, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Action for connect button
        btnConnect.addActionListener(e -> {
            String ip = txtIP.getText();
            int port = Integer.parseInt(txtPort.getText());

            if (client.connect(ip, port)) {
                textArea.append("Connected to server at " + ip + ":" + port + "\n");
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);
                btnSend.setEnabled(true);
                btnRequestInfo.setEnabled(true);
                txtMessage.setEnabled(true);

                // Start a background thread to read server messages
                new Thread(() -> {
                    while (client.isConnected()) {
                        String msg = client.readMessage();
                        if (msg != null) {
                            SwingUtilities.invokeLater(() -> {
                                textArea.append(msg + "\n");

                                // Update coordinator label based on message
                                if (msg.contains("You are now the coordinator") || msg.contains("You are the coordinator")) {
                                    lblCoordinator.setText("Coordinator: You");
                                } else if (msg.startsWith("Current Coordinator:")) {
                                    String coord = msg.replace("Current Coordinator:", "").trim();
                                    lblCoordinator.setText("Coordinator: " + coord);
                                } else if (msg.startsWith("[COORDINATOR_CHANGED]")) {
                                    String coord = msg.replace("[COORDINATOR_CHANGED]", "").trim();
                                    if (!lblCoordinator.getText().contains("You")) {
                                        lblCoordinator.setText("Coordinator: " + coord);
                                    }
                                }
                            });
                        }
                    }
                }).start();

            } else {
                textArea.append("Failed to connect to server.\n");
            }
        });

        // Action for send button
        btnSend.addActionListener(e -> {
            String msg = txtMessage.getText().trim();
            if (!msg.isEmpty()) {
                client.sendMessage(msg);
                txtMessage.setText(""); // Clear after sending
            }
        });

        // Action for disconnect button
        btnDisconnect.addActionListener(e -> {
            if (client != null && client.isConnected()) {
                client.disconnect();
                textArea.append("Disconnected from server.\n");
                btnConnect.setEnabled(true);
                btnDisconnect.setEnabled(false);
                btnSend.setEnabled(false);
                btnRequestInfo.setEnabled(false);
                txtMessage.setEnabled(false);
                lblCoordinator.setText("Coordinator: Not connected");
            }
        });

        // Action for requesting coordinator info
        btnRequestInfo.addActionListener(e -> {
            client.sendMessage("!requestinfo");
            textArea.append("Request sent to coordinator...\n");
        });

        return panel;
    }

    // Append message to GUI text area (used by server UI)
    public void logToTextArea(String message) {
        if (textArea != null) {
            textArea.append(message + "\n");
        }
    }
}
