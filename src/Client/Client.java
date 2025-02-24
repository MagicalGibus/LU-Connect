package Client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;

public class Client {
    private Socket socket;
    private String name;
    private PrintWriter writer;
    private BufferedReader reader;
    private JList<String> messages;
    private ReceiveThread receiveThread; // Handles incoming messages

    public Client(String host, int port) {
        try {
            // Attempts to connect to server
            socket = new Socket();
            System.out.println("Trying to connect to " + host + ":" + port + "...");
            socket.connect(new InetSocketAddress(host, port));
            System.out.println("Successfully connected to " + host + ":" + port);

            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Receives data
            
            // Get user's name
            name = JOptionPane.showInputDialog("Your name:");
            System.out.println("\nWelcome, " + name + "! Getting ready to send and receive messages...");
            
            // Send join message
            writer.println("Server: " + name + " has joined the chat. Say hi!");
            
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            System.exit(1);
        }
    }

    // Method to send messages, if client types "QUIT" then it closes the socket
    public void sendMessage(String message) {
        if (message.equals("QUIT")) {
            writer.println("Server: " + name + " has left the chat.");
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
            System.exit(0);
        } else {
            writer.println(name + ": " + message);
        }
    }

    // Creates GUI for client
    public void createAndShowGUI() {
        JFrame frame = new JFrame("Chatroom");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 550);

        // Scrollable area where messages are shown
        DefaultListModel<String> listModel = new DefaultListModel<>();
        messages = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(messages);
        
        // Text input area
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField textInput = new JTextField("Write your message here.");
        JButton sendButton = new JButton("Send");

        // Layout
        inputPanel.add(textInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Action listeners
        textInput.addActionListener(e -> {
            String message = textInput.getText();
            sendMessage(message);
            DefaultListModel<String> model = (DefaultListModel<String>) messages.getModel();
            model.addElement(name + ": " + message);
            textInput.setText("");
        });

        sendButton.addActionListener(e -> {
            String message = textInput.getText();
            sendMessage(message);
            DefaultListModel<String> model = (DefaultListModel<String>) messages.getModel();
            model.addElement(name + ": " + message);
            textInput.setText("");
        });

        // Start receive thread
        receiveThread = new ReceiveThread(reader, messages);
        receiveThread.start();

        frame.setVisible(true);
    }
}
