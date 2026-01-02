package server;

import client.ClientGUI;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ServerGUI {

    // Main application frame
    private JFrame frame;

    // Input field to enter server port and label to display server status
    private JTextField portField;
    private JLabel statusLabel;

    // Reference to the actual server and the thread it's running on
    private Server server;
    private Thread serverThread;

    // Components to display and manage client panels
    private DefaultListModel<String> clientListModel;
    private JList<String> clientList;
    private JPanel clientPanelContainer;
    private CardLayout cardLayout;

    // Track client GUIs and their names
    private Map<String, JPanel> clientPanels = new HashMap<>();
    private Map<String, ClientGUI> guiMap = new HashMap<>();
    private Map<String, String> nameMap = new HashMap<>();

    // Used for assigning names to clients and managing coordinator logic
    private int clientCount = 0;
    private String coordinatorName = null;
    private ClientGUI coordinatorGui = null;
    private Timer coordinatorTimer = null;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                ServerGUI window = new ServerGUI();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public ServerGUI() {
        initialize();
    }

    // Set up all GUI components and layout
    private void initialize() {
        frame = new JFrame("Chat Server GUI");
        frame.setBounds(100, 100, 800, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        frame.getContentPane().add(topPanel, BorderLayout.NORTH);

        // Controls to start/stop the server and add new clients
        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("6666", 10);
        topPanel.add(portField);

        JButton btnStart = new JButton("Start Server");
        topPanel.add(btnStart);

        JButton btnAddClient = new JButton("Add Client");
        topPanel.add(btnAddClient);

        JButton btnStop = new JButton("Stop Server");
        topPanel.add(btnStop);

        statusLabel = new JLabel("Status: Not running");
        topPanel.add(statusLabel);

        // List of clients on the right-hand side
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        JScrollPane listScrollPane = new JScrollPane(clientList);
        listScrollPane.setPreferredSize(new Dimension(180, 0));
        frame.getContentPane().add(listScrollPane, BorderLayout.EAST);

        // Panel container for dynamically switching between clients
        cardLayout = new CardLayout();
        clientPanelContainer = new JPanel(cardLayout);
        frame.getContentPane().add(clientPanelContainer, BorderLayout.CENTER);

        // Handle starting the server
        btnStart.addActionListener(e -> {
            int port = Integer.parseInt(portField.getText());
            if (server == null) {
                server = new Server();
                serverThread = new Thread(() -> server.start(port));
                serverThread.start();
                statusLabel.setText("Server started on port " + port);
            } else {
                JOptionPane.showMessageDialog(frame, "Server already running.");
            }
        });

        // Handle adding a new client
        btnAddClient.addActionListener(e -> {
            if (server == null) {
                JOptionPane.showMessageDialog(frame, "Start the server first.");
            } else {
                addNewClient();
            }
        });

        // Handle stopping the server and cleaning up
        btnStop.addActionListener(e -> {
            if (server != null) {
                server.stop();
                server = null;
                stopCoordinatorCheck();
                clientListModel.clear();
                clientPanels.clear();
                nameMap.clear();
                guiMap.clear();
                clientPanelContainer.removeAll();
                statusLabel.setText("Server stopped.");
            }
        });

        // Allow switching between different client panels
        clientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String displayName = clientList.getSelectedValue();
                if (displayName != null) {
                    for (Map.Entry<String, String> entry : nameMap.entrySet()) {
                        if (entry.getValue().equals(displayName)) {
                            cardLayout.show(clientPanelContainer, entry.getKey());
                            break;
                        }
                    }
                }
            }
        });
    }

    // Adds a new client GUI panel and tracks its info
    private void addNewClient() {
        String clientName = "Client " + (++clientCount);
        ClientGUI clientGui = new ClientGUI();
        JPanel clientPanel = clientGui.createClientPanel(clientName, "");

        clientPanels.put(clientName, clientPanel);
        clientPanelContainer.add(clientPanel, clientName);
        nameMap.put(clientName, clientName);
        guiMap.put(clientName, clientGui);
        clientListModel.addElement(clientName);
        clientList.setSelectedValue(clientName, true);

        // Handle disconnect button click
        JButton disconnectBtn = getDisconnectButton(clientPanel);
        if (disconnectBtn != null) {
            disconnectBtn.addActionListener(e -> {
                if (clientName.equals(coordinatorName)) {
                    stopCoordinatorCheck();
                    coordinatorGui = null;
                    coordinatorName = null;
                    promoteNextCoordinator();
                }

                clientPanels.remove(clientName);
                nameMap.remove(clientName);
                guiMap.remove(clientName);
                clientListModel.removeElement(clientName);
                clientPanelContainer.remove(clientPanel);
                clientPanelContainer.revalidate();
                clientPanelContainer.repaint();
            });
        }

        // Handle connect button click
        JButton connectBtn = getConnectButton(clientPanel);
        if (connectBtn != null) {
            connectBtn.addActionListener(e -> {
                if (coordinatorName == null) {
                    coordinatorName = clientName;
                    coordinatorGui = clientGui;
                    updateClientListDisplay();
                    startCoordinatorCheck();
                }
            });
        }

        clientPanelContainer.revalidate();
        clientPanelContainer.repaint();
    }

    // Promotes the next available client as coordinator
    private void promoteNextCoordinator() {
        for (int i = 0; i < clientListModel.size(); i++) {
            String clientName = clientListModel.getElementAt(i);
            if (!clientName.contains("(Coordinator)")) {
                coordinatorName = clientName;
                coordinatorGui = guiMap.get(clientName);
                updateClientListDisplay();
                startCoordinatorCheck();
                break;
            }
        }
    }

    // Updates the list on the right to reflect the new coordinator
    private void updateClientListDisplay() {
        clientListModel.clear();
        for (String clientName : guiMap.keySet()) {
            String display = clientName;
            if (clientName.equals(coordinatorName)) {
                display += " (Coordinator)";
            }
            nameMap.put(clientName, display);
            clientListModel.addElement(display);
        }
    }

    // Utility method to find the Disconnect button in a client panel
    private JButton getDisconnectButton(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel inner) {
                for (Component nested : inner.getComponents()) {
                    if (nested instanceof JButton btn && btn.getText().equals("Disconnect")) {
                        return btn;
                    }
                }
            }
        }
        return null;
    }

    // Utility method to find the Connect button in a client panel
    private JButton getConnectButton(JPanel panel) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JPanel inner) {
                for (Component nested : inner.getComponents()) {
                    if (nested instanceof JButton btn && btn.getText().equals("Connect")) {
                        return btn;
                    }
                }
            }
        }
        return null;
    }

    // Starts a repeating task that lets the coordinator print the client list
    private void startCoordinatorCheck() {
        stopCoordinatorCheck();
        if (coordinatorGui == null) return;

        coordinatorTimer = new Timer(true);
        coordinatorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (coordinatorGui != null) {
                        StringBuilder builder = new StringBuilder("Active Clients:\n");
                        for (int i = 0; i < clientListModel.size(); i++) {
                            builder.append(" - ").append(clientListModel.getElementAt(i)).append("\n");
                        }
                        coordinatorGui.logToTextArea(builder.toString());
                    }
                });
            }
        }, 0, 20000); // Every 20 seconds
    }

    // Stops the coordinator update timer
    private void stopCoordinatorCheck() {
        if (coordinatorTimer != null) {
            coordinatorTimer.cancel();
            coordinatorTimer = null;
        }
    }
}
