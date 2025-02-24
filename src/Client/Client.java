package Client;

import Encryption.EncryptionTool;

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
            
            // First message from server will be the encryption key
            String keyStr = reader.readLine();
            EncryptionTool.setKeyFromString(keyStr);
            
            // Handle authentication
            handleAuthentication();
            
            System.out.println("\nWelcome, " + name + "! Getting ready to send and receive messages...");
            
            // Send encrypted join message
            String joinMessage = "Server: " + name + " has joined the chat. Say hi!";
            writer.println(EncryptionTool.encrypt(joinMessage));
            
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            System.exit(1);
        }
    }

    // Handles login/register
    private void handleAuthentication() throws Exception {
        // Create GUI for login/register
        String[] options = {"Login", "Register"};
        int choice = JOptionPane.showOptionDialog(
            null, 
            "Choose an option:", 
            "Authentication", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, 
            options, 
            options[0]
        );
        
        if (choice == 0) { // Login
            login();
        } else { // Register
            register();
        }
    }
    
    // Handle login process
    private void login() throws Exception {
        boolean authenticated = false;
        while (!authenticated) {
            JPanel panel = new JPanel(new GridLayout(2, 2));
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);
            
            int result = JOptionPane.showConfirmDialog(null, panel, "Login", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION) {
                socket.close();
                System.exit(0);
            }
            
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Username and password cannot be empty");
                continue;
            }
            
            // Send login request 
            writer.println(EncryptionTool.encrypt("AUTH_LOGIN:" + username + ":" + password));
            
            // Wait for server response
            String response = EncryptionTool.decrypt(reader.readLine());
            if (response.equals("AUTH_SUCCESS")) {
                authenticated = true;
                name = username;
                JOptionPane.showMessageDialog(null, "Login successful!");
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password");
            }
        }
    }
    
    // Handles registration process
    private void register() throws Exception {
        boolean registered = false;
        while (!registered) {
            JPanel panel = new JPanel(new GridLayout(3, 2));
            JTextField usernameField = new JTextField();
            JPasswordField passwordField = new JPasswordField();
            JPasswordField confirmPasswordField = new JPasswordField();
            
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);
            panel.add(new JLabel("Confirm Password:"));
            panel.add(confirmPasswordField);
            
            int result = JOptionPane.showConfirmDialog(null, panel, "Register", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.CANCEL_OPTION) {
                socket.close();
                System.exit(0);
            }
            
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Username and password cannot be empty");
                continue;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(null, "Passwords do not match");
                continue;
            }
            
            // Send registration request to server
            writer.println(EncryptionTool.encrypt("AUTH_REGISTER:" + username + ":" + password));
            
            // Wait for server response
            String response = EncryptionTool.decrypt(reader.readLine());
            if (response.equals("AUTH_SUCCESS")) {
                registered = true;
                name = username;
                JOptionPane.showMessageDialog(null, "Registration successful!");
            } else {
                JOptionPane.showMessageDialog(null, "Username already exists");
            }
        }
    }

    // Method to send messages, if client types "QUIT" then it closes the socket
    public void sendMessage(String message) {
        try {
            if (message.equals("QUIT")) {
                String exitMessage = "Server: " + name + " has left the chat.";
                writer.println(EncryptionTool.encrypt(exitMessage));
                socket.close();
                System.exit(0);
            } else {
                String fullMessage = name + ": " + message;
                String encryptedMessage = EncryptionTool.encrypt(fullMessage);
                writer.println(encryptedMessage);
            }
        } catch (Exception e) {
            System.err.println("Error encrypting message: " + e.getMessage());
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