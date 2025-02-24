package Encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class EncryptionTool {
    private static final String ALGORITHM = "AES"; // Advanced Encrpytion Standard algorithm
    private static SecretKey secretKey; // Holds encryption key
    
    static {
        try {
            // Generates a random 256-bit key
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256); // 256-bit key 
            secretKey = keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption", e);
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