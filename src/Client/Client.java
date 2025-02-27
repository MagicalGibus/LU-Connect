package Client;

import Encryption.EncryptionTool;
import FileTransfer.FileTransferManager;
import FileTransfer.FileTransferProtocol;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.*;


public class Client implements FileTransferManager.FileTransferCallback {
    private Socket socket;
    private String name;
    private PrintWriter writer;
    private BufferedReader reader;
    private JList<String> messages;
    private ReceiveThread receiveThread; // Handles incoming messages
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
    private FileTransferManager fileTransferManager;
    private JProgressBar fileProgressBar;
    private JLabel fileStatusLabel;
    private Map<String, JButton> fileViewButtons = new HashMap<>();
    private DefaultListModel<String> listModel;

    public Client(String host, int port) {
        try {
            // Attempts to connect to server
            socket = new Socket();
            System.out.println("Trying to connect to " + host + ":" + port + "...");
            socket.connect(new InetSocketAddress(host, port));
            System.out.println("Successfully connected to " + host + ":" + port);

            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Receives data
            
            fileTransferManager = new FileTransferManager(this);
            
            // Check server status
            String serverMessage = reader.readLine();
            
            if (serverMessage.startsWith("SERVER_FULL:")) {
                // Show message and exit
                String errorMessage = serverMessage.substring("SERVER_FULL:".length());
                JOptionPane.showMessageDialog(null, errorMessage, "Server Full", JOptionPane.ERROR_MESSAGE);
                socket.close();
                System.exit(1);
            } else if (serverMessage.startsWith("SERVER_WAITING:")) {
                // Show waiting message
                String waitingMessage = serverMessage.substring("SERVER_WAITING:".length());
                JOptionPane.showMessageDialog(null, waitingMessage, "Waiting for Server", JOptionPane.INFORMATION_MESSAGE);
                
                // Create and show a waiting dialogue
                JDialog waitDialogue = new JDialog((Frame)null, "Waiting in Queue", true);
                waitDialogue.setLayout(new BorderLayout());
                JLabel waitLabel = new JLabel("Waiting for a spot on the server...", JLabel.CENTER);
                JButton cancelButton = new JButton("Cancel");
                
                cancelButton.addActionListener(e -> {
                    try {
                        socket.close();
                        System.exit(0);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
                
                waitDialogue.add(waitLabel, BorderLayout.CENTER);
                waitDialogue.add(cancelButton, BorderLayout.SOUTH);
                waitDialogue.setSize(300, 150);
                waitDialogue.setLocationRelativeTo(null);
                
                // Start a thread to wait for server response
                new Thread(() -> {
                    try {
                        String response = reader.readLine();
                        if (response.startsWith("SERVER_CONNECTED:")) {
                        
                            SwingUtilities.invokeLater(() -> waitDialogue.dispose());
                            
                            // Wait for the encryption key in next message 
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }).start();
                
                waitDialogue.setVisible(true);
                serverMessage = reader.readLine();
            }
            
            // serverMessage now contains the encryption key
            System.out.println("Setting encryption key...");
            EncryptionTool.setKeyFromString(serverMessage);

            handleAuthentication();
            
            System.out.println("\nWelcome, " + name + "! Getting ready to send and receive messages...");
            
            // Sends encrypted join message with timestamp
            String timestamp = LocalDateTime.now().format(timeFormatter);
            String joinMessage = timestamp + " Server: " + name + " has joined the chat. Say hi!";
            writer.println(EncryptionTool.encrypt(joinMessage));
            
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // Handles login/register
    private void handleAuthentication() throws Exception {
        // Creates GUI for login/register
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
        
        if (choice == 0) { 
            login();
        } else { 
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
                String timestamp = LocalDateTime.now().format(timeFormatter);
                String exitMessage = timestamp + " Server: " + name + " has left the chat.";
                writer.println(EncryptionTool.encrypt(exitMessage));
                socket.close();
                System.exit(0);
            } else {
                String timestamp = LocalDateTime.now().format(timeFormatter);
                String fullMessage = timestamp + " " + name + ": " + message;
                String encryptedMessage = EncryptionTool.encrypt(fullMessage);
                writer.println(encryptedMessage);
            }
        } catch (Exception e) {
            System.err.println("Error encrypting message: " + e.getMessage());
        }
    }

    // For sending files
    private FileTransferManager.SendCallback createSendCallback() {
        return new FileTransferManager.SendCallback() {
            @Override
            public void send(String message) {
                writer.println(message);
            }
        };
    }

    // Method to handle file selection and sending
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Send");
        
        // Set file filter for allowed file types
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Documents & Images (*.docx, *.pdf, *.jpeg, *.jpg)", "docx", "pdf", "jpeg", "jpg");
        fileChooser.setFileFilter(filter);
        
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            
            // Validate file
            if (!FileTransferProtocol.isValidFile(selectedFile)) {
                JOptionPane.showMessageDialog(null, 
                    "Invalid file. Only .docx, .pdf, and .jpeg/.jpg files under 10MB are allowed.", 
                    "File Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Update UI
            fileStatusLabel.setText("Sending: " + selectedFile.getName());
            fileProgressBar.setValue(0);
            fileProgressBar.setVisible(true);
            
            // Add message to chat
            String timestamp = LocalDateTime.now().format(timeFormatter);
            listModel.addElement(timestamp + " " + name + " is sending file: " + selectedFile.getName());
            
            fileTransferManager.sendFile(selectedFile, createSendCallback());
        }
    }

    // Creates GUI for client
    public void createAndShowGUI() {
        JFrame frame = new JFrame("Chatroom - " + name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Scrollable area where messages are shown
        listModel = new DefaultListModel<>();
        messages = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(messages);
        
        // Text input area
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField textInput = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton fileButton = new JButton("Send File");

        // File transfer status panel
        JPanel filePanel = new JPanel(new BorderLayout());
        fileStatusLabel = new JLabel("No file transfer in progress");
        fileProgressBar = new JProgressBar(0, 100);
        fileProgressBar.setStringPainted(true);
        fileProgressBar.setVisible(false);
        
        filePanel.add(fileStatusLabel, BorderLayout.NORTH);
        filePanel.add(fileProgressBar, BorderLayout.CENTER);
        
        // Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);
        
        inputPanel.add(textInput, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(inputPanel, BorderLayout.CENTER);
        southPanel.add(filePanel, BorderLayout.SOUTH);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(southPanel, BorderLayout.SOUTH);

        // Action listeners
        textInput.addActionListener(e -> {
            String message = textInput.getText();
            if (!message.trim().isEmpty()) {
                // Adds the message to local display with timestamp
                String timestamp = LocalDateTime.now().format(timeFormatter);
                listModel.addElement(timestamp + " " + name + ": " + message);
                
                sendMessage(message);
                textInput.setText("");
            }
        });

        sendButton.addActionListener(e -> {
            String message = textInput.getText();
            if (!message.trim().isEmpty()) {
                // Adds the message to local display with timestamp
                String timestamp = LocalDateTime.now().format(timeFormatter);
                listModel.addElement(timestamp + " " + name + ": " + message);
                
                // Send the message to server
                sendMessage(message);
                textInput.setText("");
            }
        });
        
        fileButton.addActionListener(e -> sendFile());

        // Start receive thread
        receiveThread = new ReceiveThread(reader, messages, fileTransferManager);
        receiveThread.start();

        frame.setVisible(true);
    }
    
    // FileTransferCallback implementations
    @Override
    public void onTransferProgress(String fileName, int progress) {
        SwingUtilities.invokeLater(() -> {
            fileStatusLabel.setText("File transfer in progress: " + fileName);
            fileProgressBar.setValue(progress);
            fileProgressBar.setVisible(true);
        });
    }

    @Override
    public void onTransferComplete(String fileName, File file) {
        SwingUtilities.invokeLater(() -> {
            fileStatusLabel.setText("Transfer complete: " + fileName);
            fileProgressBar.setValue(100);
            
            // Add message to chat
            String timestamp = LocalDateTime.now().format(timeFormatter);
            listModel.addElement(timestamp + " File received: " + fileName);
            
            // Add a button to view/open the file
            JButton viewButton = new JButton("Open File");
            viewButton.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, 
                        "Error opening file: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            
            // Store the button for this file
            fileViewButtons.put(fileName, viewButton);
            
            // Delay hiding the progress bar
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    SwingUtilities.invokeLater(() -> {
                        fileProgressBar.setVisible(false);
                        fileStatusLabel.setText("No file transfer in progress");
                    });
                } catch (InterruptedException ex) {
                    // Ignore
                }
            }).start();
        });
    }

    @Override
    public void onTransferError(String fileName, String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            fileStatusLabel.setText("Transfer error: " + errorMessage);
            fileProgressBar.setVisible(false);
            
            // Send error in chat
            String timestamp = LocalDateTime.now().format(timeFormatter);
            listModel.addElement(timestamp + " File transfer error: " + errorMessage);
        });
    }
}