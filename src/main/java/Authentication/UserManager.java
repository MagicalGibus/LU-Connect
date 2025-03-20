package Authentication;

import Encryption.EncryptionTool;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private static final String USER_FILE = "users.dat";
    private Map<String, String> users; // username -> encrypted password

    public UserManager() {
        users = new HashMap<>();
        loadUsers();
    }

    // Load existing users from file
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_FILE))) {
            users = (Map<String, String>) ois.readObject();
            System.out.println("Loaded " + users.size() + " user accounts");
        } catch (FileNotFoundException e) {
            System.out.println("No existing user file found. Creating new user database.");
            saveUsers(); // Create the file
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading user data");
            users = new HashMap<>();
            saveUsers();
        }
    }

    // Save users to file
    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.out.println("Error saving user data");
        }
    }

    // Register a new user
    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false; // Username already exists
        }

        try {
            // Encrypt the password before storing
            String encryptedPassword = EncryptionTool.encrypt(password);
            users.put(username, encryptedPassword);
            saveUsers();
            return true;
        } catch (Exception e) {
            System.out.println("Error encrypting password");
            return false;
        }
    }

    // Authenticate a user
    public boolean authenticateUser(String username, String password) {
        if (!users.containsKey(username)) {
            return false; // User doesn't exist
        }

        try {
            String storedEncryptedPassword = users.get(username);
            String decryptedStoredPassword = EncryptionTool.decrypt(storedEncryptedPassword);
            return decryptedStoredPassword.equals(password);
        } catch (Exception e) {
            System.out.println("Error authenticating user: " + e.getMessage());
            return false;
        }
    }
}
