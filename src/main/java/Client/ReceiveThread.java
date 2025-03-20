package Client;

import Encryption.EncryptionTool;
import FileTransfer.FileTransfer;

import java.io.BufferedReader;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

public class ReceiveThread extends Thread {
    private BufferedReader reader; // To store incoming messages
    private JList<String> messages; // To display messages in GUI
    private boolean running = true;
    private FileTransfer fileTransfer;
    private NotificationSound notificationSound; 

    public ReceiveThread(BufferedReader reader, JList<String> messages, FileTransfer fileTransfer, 
                         NotificationSound notificationSound) {
        this.reader = reader;
        this.messages = messages;
        this.fileTransfer = fileTransfer;
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
                    
                    // Check if this is a file transfer message
                    if (decryptedMessage.startsWith(FileTransfer.FILE_START) || 
                        decryptedMessage.startsWith(FileTransfer.FILE_CHUNK) || 
                        decryptedMessage.startsWith(FileTransfer.FILE_END) || 
                        decryptedMessage.startsWith(FileTransfer.FILE_ERROR)) {
                        
                        // Handle file transfer message
                        fileTransfer.processFileMessage(decryptedMessage);
                        
                        // Play notification sound for file transfer start
                        if (decryptedMessage.startsWith(FileTransfer.FILE_START)) {
                            notificationSound.playNotificationSound();
                        }
                    } else {
                        // Normal chat message
                        SwingUtilities.invokeLater(() -> { 
                            DefaultListModel<String> model = (DefaultListModel<String>) messages.getModel();
                            model.addElement(decryptedMessage);
                            messages.ensureIndexIsVisible(model.getSize() - 1);
                            
                            // Plays a notification sound for new messages but doesn't play when the user sends a message themself
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