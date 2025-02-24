package Encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Base64;

public class EncryptionTool {
    private static final String ALGORITHM = "AES"; // Advanced Encryption Standard algorithm
    private static SecretKey secretKey; // Holds encryption key
    private static final String KEY_FILE = "server_key.dat"; // File to store the key
    
    static {
        try {
            // Try to load existing key
            if (loadKeyFromFile()) {
                System.out.println("Loaded existing encryption key");
            } else {
                // Generate a new key if no existing key is found
                KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                keyGen.init(256); // 256-bit key 
                secretKey = keyGen.generateKey();
                saveKeyToFile(); // Save new key
                System.out.println("Generated and saved new encryption key");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }
    
    // Save the current key to a file
    private static void saveKeyToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(KEY_FILE))) {
            oos.writeObject(secretKey.getEncoded());
        } catch (IOException e) {
            System.out.println("Error saving encryption key: " + e.getMessage());
        }
    }
    
    // Load the key from file
    private static boolean loadKeyFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(KEY_FILE))) {
            byte[] keyData = (byte[]) ois.readObject();
            secretKey = new SecretKeySpec(keyData, 0, keyData.length, ALGORITHM);
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("No existing encryption key file found.");
            return false;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading encryption key: " + e.getMessage());
            return false;
        }
    }
    
    public static String encrypt(String message) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(message.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }
    
    public static String decrypt(String encryptedMessage) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
        return new String(decryptedBytes);
    }
    
    // Method to get the current key as a string (for sharing with clients)
    public static String getKeyAsString() {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
    
    // Method to set the key from a string (for clients to use the server's key)
    public static void setKeyFromString(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }
}