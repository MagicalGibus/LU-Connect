package Client;

public class NotificationSound {
    private boolean soundEnabled = true;
    
    public NotificationSound() {
        System.out.println("Sound system initialized using system beep");
    }
    
    public void playNotificationSound() {
        if (!soundEnabled) {
            return;
        }
        
        try {
            // Use system beep 
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            System.err.println("Error playing system beep: " + e.getMessage());
        }
    }
    
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }
    
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    public boolean isSoundInitialized() {
        return true;
    }
}