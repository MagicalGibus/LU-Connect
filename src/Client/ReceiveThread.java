package Client;

import java.io.BufferedReader;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.SwingUtilities;

public class ReceiveThread extends Thread {
    private BufferedReader reader; // To store incoming messages
    private JList<String> messages; // To display messages in GUI
    private boolean running = true;

    public ReceiveThread(BufferedReader reader, JList<String> messages) {
        this.reader = reader;
        this.messages = messages;
    }

    @Override
    public void run() {
        try {
            String message;
            // Continuous loop while there are messages to be read and the server is running
            while (running && (message = reader.readLine()) != null) {
                final String finalMessage = message; // Unchangeable copy of message for lambda function
                SwingUtilities.invokeLater(() -> { // Necessary for thread safety, ensures running on event dispatch thread 
                    DefaultListModel<String> model = (DefaultListModel<String>) messages.getModel();
                    model.addElement(finalMessage);
                    messages.ensureIndexIsVisible(model.getSize() - 1); // Scrolls page down so new message is shown
                });
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error receiving message: " + e.getMessage());
            }
        }
    }

    // Stops the threads execution
    public void stopRunning() {
        running = false;
    }
}