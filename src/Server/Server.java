package Server;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import Encryption.EncryptionTool;

public class Server extends Thread {
    private List<ClientHandler> clients = new ArrayList<ClientHandler>(); // Tracks all connected clients
    private String host;
    private int port;
    private ServerSocket serverSocket;
    private ConnectionSemaphore connectionSemaphore;
    private volatile boolean running = true;

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
        this.connectionSemaphore = new ConnectionSemaphore();
    }

    @Override
    public void run(){
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(host, port)); // Binds socket to specified host and port

            System.out.println("Server started at " + serverSocket.getLocalSocketAddress());
            System.out.println("Connection limit: 3 users at a time");
            System.out.println("Waiting for client connections...");
            
            // Start the exit handler
            ExitHandler exitHandler = new ExitHandler(this);
            exitHandler.start();

            while (running && !serverSocket.isClosed()){
                Socket clientSocket = serverSocket.accept(); // Waits for clients to connect
                System.out.println("New connection attempt from " + clientSocket.getRemoteSocketAddress() + " to " + clientSocket.getLocalSocketAddress());

                // Create a handler thread for this connection attempt
                new Thread(() -> handleConnectionAttempt(clientSocket)).start();
            }
        } catch(IOException e) {
            if (!serverSocket.isClosed() && running) {
                System.out.println("Server error: " + e.getMessage());
            }
        }
    }

    // Handles connection attempt in a separate thread
    private void handleConnectionAttempt(Socket clientSocket) {
        try {
            SocketAddress clientAddress = clientSocket.getRemoteSocketAddress();
            PrintWriter tempWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            
            // Check current server status
            int activeConnections = 3 - connectionSemaphore.getAvailableConnections();
            boolean wasWaiting = false;
            
            if (activeConnections >= 3) {
                wasWaiting = true;
                System.out.println("Server full - Client " + clientAddress + " will wait in queue");
                tempWriter.println("SERVER_WAITING:Server is currently full. You are in a waiting queue. Please wait...");
            }
            
            // Try to connect, will fail if the server is full
            try {
                connectionSemaphore.acquireConnection();
                
                System.out.println("Connection granted to " + clientAddress + 
                                  " - " + (3 - connectionSemaphore.getAvailableConnections()) + "/3 connections active");
                
                if (wasWaiting) {
                    tempWriter.println("SERVER_CONNECTED:You've been granted access to the server.");
                    
                    Thread.sleep(100);
                    
                    // Send encryption key
                    tempWriter.println(EncryptionTool.getKeyAsString());
                    System.out.println("Sent encryption key to previously waiting client");
                } else {
                    // Pass
                }
                
                // Create and start client handler, say if client is waiting in queue or not
                ClientHandler clientHandler = new ClientHandler(clientSocket, this, wasWaiting);
                synchronized (clients) {
                    clients.add(clientHandler);
                }
                clientHandler.start();
                
            } catch (InterruptedException e) {
                System.out.println("Connection attempt interrupted for " + clientAddress);
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error handling connection attempt: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    // Sends message to all connected clients
    public void broadcast(String message, SocketAddress source) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (!client.getSocketAddress().equals(source)) {
                    client.send(message);
                }
            }
        }
    }
    
    // Removes a client handler from the list when it disconnects
    public void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        // Release the semaphore permit when a client disconnects
        connectionSemaphore.releaseConnection();
        System.out.println("Client removed. Active connections: " + (3 - connectionSemaphore.getAvailableConnections()) + 
                          "/3, Waiting clients: " + connectionSemaphore.getQueueLength());
    }

    // Getter method to return list of all connected clients
    public List<ClientHandler> getClients() {
        synchronized (clients) {
            return new ArrayList<>(clients);
        }
    }

    // Shuts down server
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server shutdown complete.");
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }
    }
}