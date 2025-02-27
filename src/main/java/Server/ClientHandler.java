package Server;

import Authentication.UserManager;
import Encryption.EncryptionTool;
import FileTransfer.FileTransferProtocol;

import java.net.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private SocketAddress socketAddress;
    private Server server;
    private PrintWriter writer;
    private String username;
    private static UserManager userManager = new UserManager();
    private boolean clientWasQueued;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("[HH:mm:ss]");
    

    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.socketAddress = clientSocket.getRemoteSocketAddress();
        this.server = server; // The main server
        this.clientWasQueued = false;
    }
    
    // Constructor for clients that were in waiting queue
    public ClientHandler(Socket clientSocket, Server server, boolean wasQueued) {
        this(clientSocket, server);
        this.clientWasQueued = wasQueued;
    }

    @Override
    public void run() {
        try {
            // Reads text from client
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true); // Sends text to client

            // Send encryption key if this client not in queue
            if (!clientWasQueued) {
                System.out.println("Sending encryption key to new client");
                writer.println(EncryptionTool.getKeyAsString());
            } else {
                System.out.println("Client queued");
            }

            // Handles authentication 
            boolean authenticated = false;
            while (!authenticated) {
                String encryptedAuthMessage = reader.readLine();
                if (encryptedAuthMessage == null) {
                    System.out.println("Client disconnected during authentication");
                    break; // Client disconnected
                }
                
                try {
                    String authMessage = EncryptionTool.decrypt(encryptedAuthMessage);
                    System.out.println("Received auth message: " + authMessage);
                    String[] parts = authMessage.split(":");
                    
                    if (parts.length >= 3) {
                        String authType = parts[0];
                        String username = parts[1];
                        String password = parts[2];
                        
                        if (authType.equals("AUTH_LOGIN")) {
                            if (userManager.authenticateUser(username, password)) {
                                authenticated = true;
                                this.username = username;
                                writer.println(EncryptionTool.encrypt("AUTH_SUCCESS"));
                                System.out.println("Login successful for: " + username);
                            } else {
                                writer.println(EncryptionTool.encrypt("AUTH_FAILURE"));
                                System.out.println("Login failed for: " + username);
                            }
                        } else if (authType.equals("AUTH_REGISTER")) {
                            if (userManager.registerUser(username, password)) {
                                authenticated = true;
                                this.username = username;
                                writer.println(EncryptionTool.encrypt("AUTH_SUCCESS"));
                                System.out.println("Registration successful for: " + username);
                            } else {
                                writer.println(EncryptionTool.encrypt("AUTH_FAILURE"));
                                System.out.println("Registration failed for: " + username);
                            }
                        }
                    } else {
                        System.out.println("Invalid auth message format: " + authMessage);
                    }
                } catch (Exception e) {
                    System.err.println("Error during authentication: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            if (!authenticated) {
                clientSocket.close();
                server.removeClient(this);
                return;
            }

            String message;
            while ((message = reader.readLine()) != null) {
                try {
                    String decryptedMessage = EncryptionTool.decrypt(message);
                    
                    // Check if this is a file transfer message
                    if (decryptedMessage.startsWith(FileTransferProtocol.FILE_START)) {
                        // File start notification, add a system message
                        String[] parts = decryptedMessage.split(":", 3);
                        if (parts.length >= 3) {
                            String fileName = parts[1];
                            String timestamp = LocalDateTime.now().format(timeFormatter);
                            String fileNotification = timestamp + " Server: " + username + " is sending file: " + fileName;
                            
                            server.broadcast(EncryptionTool.encrypt(fileNotification), null);
                        }
                    }
                    
                    // Pass the message along, whether it's a chat message or file transfer
                    System.out.println(socketAddress + " (" + username + ") sent a message/file data");
                    server.broadcast(message, socketAddress);
                } catch (Exception e) {
                    System.err.println("Error processing message: " + e.getMessage());
                }
            }

            // Print info when client disconnects
            System.out.println(socketAddress + " (" + username + ") has closed the connection");
            clientSocket.close();
            server.removeClient(this);
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                System.out.println("Error closing socket: " + ex.getMessage());
            }
            server.removeClient(this);
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            server.removeClient(this);
        }
    }

    // Send message to this client
    public void send(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // Getter for address
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    // Getter for username
    public String getUsername() {
        return username;
    }

    // Closes the client socket
    public void closeSocket() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }
}