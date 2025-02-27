package Client;

import Encryption.EncryptionTool;
import FileTransfer.FileTransferManager;
import FileTransfer.FileTransferProtocol;

import java.io.BufferedReader;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

public class ReceiveThread extends Thread {
    private BufferedReader reader; // To store incoming messages
    private JList<String> messages; // To display messages in GUI
    private boolean running = true;
    private FileTransferManager fileTransferManager;
    private NotificationSound notificationSound; // Added notification sound

    public ReceiveThread(BufferedReader reader, JList<String> messages, FileTransferManager fileTransferManager, 
                         NotificationSound notificationSound) {
        this.reader = reader;
        this.messages = messages;
        this.fileTransferManager = fileTransferManager;
        this.notificationSound = notificationSound;
    }

    @Override
    public void run() {
        try {
            String encryptedMessage;
            // Continuous loop while there are messages to be read and the server is running
            while (running && (encryptedMessage = reader.readLine()) != null) {
                try {
                    final String decryptedMessage = EncryptionTool.decrypt(encryptedMessage);
                    
                    // Check if file transfer message
                    if (decryptedMessage.startsWith(FileTransferProtocol.FILE_START) || 
                        decryptedMessage.startsWith(FileTransferProtocol.FILE_CHUNK) || 
                        decryptedMessage.startsWith(FileTransferProtocol.FILE_END) || 
                        decryptedMessage.startsWith(FileTransferProtocol.FILE_ERROR)) {
                        
                        // Handle file transfer message
                        fileTransferManager.processFileMessage(decryptedMessage);
                        
                        // Play notification sound for file transfer start
                        if (decryptedMessage.startsWith(FileTransferProtocol.FILE_START)) {
                            notificationSound.playNotificationSound();
                        }
                    } else {
                        // Regular chat message - add to UI
                        SwingUtilities.invokeLater(() -> { 
                            DefaultListModel<String> model = (DefaultListModel<String>) messages.getModel();
                            model.addElement(decryptedMessage);
                            messages.ensureIndexIsVisible(model.getSize() - 1);
                            
                            // Play notification sound for new messages from others
                            // Don't play for system messages or your own messages
                            if (!decryptedMessage.contains("Server:") && !decryptedMessage.contains(": " + decryptedMessage)) {
                                notificationSound.playNotificationSound();
                            }
                        });
                    }
                } catch (Exception e) {
                    System.err.println("Error decrypting message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error receiving message: " + e.getMessage());
            }
        }
    }
    
    public void stopRunning() {
        running = false;
    }
}