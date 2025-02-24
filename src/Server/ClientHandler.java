package Server;

import Authentication.UserManager;
import Encryption.EncryptionTool;

import java.net.*;
import java.io.*;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private SocketAddress socketAddress;
    private Server server;
    private PrintWriter writer;
    private String username;
    private static UserManager userManager = new UserManager();

    public ClientHandler(Socket clientSocket, Server server) {
        this.clientSocket = clientSocket;
        this.socketAddress = clientSocket.getRemoteSocketAddress();
        this.server = server; // The main server
    }

    @Override
    public void run() {
        try {
            // Reads text from client
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new PrintWriter(clientSocket.getOutputStream(), true); // Sends text to client

            // Send encryption key
            writer.println(EncryptionTool.getKeyAsString());

            // Handles authentication 
            boolean authenticated = false;
            while (!authenticated) {
                String encryptedAuthMessage = reader.readLine();
                if (encryptedAuthMessage == null) {
                    break; // Client disconnected
                }
                
                String authMessage = EncryptionTool.decrypt(encryptedAuthMessage);
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
                        } else {
                            writer.println(EncryptionTool.encrypt("AUTH_FAILURE"));
                        }
                    } else if (authType.equals("AUTH_REGISTER")) {
                        if (userManager.registerUser(username, password)) {
                            authenticated = true;
                            this.username = username;
                            writer.println(EncryptionTool.encrypt("AUTH_SUCCESS"));
                        } else {
                            writer.println(EncryptionTool.encrypt("AUTH_FAILURE"));
                        }
                    }
                }
            }
            
            if (!authenticated) {
                clientSocket.close();
                server.removeClient(this);
                return;
            }

            // Continue with normal message handling
            String message;
            while ((message = reader.readLine()) != null) {
                // Pass message along
                System.out.println(socketAddress + " (" + username + ") sent a message");
                server.broadcast(message, socketAddress);
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
            e.printStackTrace();
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