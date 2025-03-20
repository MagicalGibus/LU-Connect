package FileTransfer;

import Encryption.EncryptionTool;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransfer {
    // Protocol constants
    public static final String FILE_START = "FILE_START";
    public static final String FILE_CHUNK = "FILE_CHUNK";
    public static final String FILE_END = "FILE_END";
    public static final String FILE_ERROR = "FILE_ERROR";
    
    // Configuration
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList(".docx", ".pdf", ".jpeg", ".jpg"));
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final int CHUNK_SIZE = 64 * 1024; // 64KB
    private static final String DOWNLOAD_DIR = "downloads";
    
    // Transfer state tracking 
    private final Map<String, Long> fileSizes = new HashMap<>();
    private final Map<String, Map<Integer, byte[]>> fileChunks = new HashMap<>();
    private final Map<String, Integer> totalChunksMap = new HashMap<>();
    private final Set<String> completedFiles = new HashSet<>();
    
    private final ExecutorService transferExecutor = Executors.newFixedThreadPool(2);
    private final FileTransferCallback callback;

    public interface FileTransferCallback {
        void onTransferProgress(String fileName, int progress);
        void onTransferComplete(String fileName, File file);
        void onTransferError(String fileName, String errorMessage);
    }
    
    public interface SendCallback {
        void send(String message);
    }
    
    public FileTransfer(FileTransferCallback callback) {
        this.callback = callback;
        
        // Create download directory if it doesn't exist
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
    }
    
    public static boolean isValidFile(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        if (file.length() > MAX_FILE_SIZE) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        for (String extension : ALLOWED_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static String getFileExtension(String fileName) {
        int lastDotPos = fileName.lastIndexOf('.');
        if (lastDotPos > 0) {
            return fileName.substring(lastDotPos).toLowerCase();
        }
        return "";
    }
    
    public static boolean isAllowedExtension(String extension) {
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    // Protocol message creation methods
    public static String createFileStartMessage(String fileName, long fileSize) {
        return FILE_START + ":" + fileName + ":" + fileSize;
    }
    
    public static String createFileChunkMessage(int chunkIndex, int totalChunks, String data) {
        return FILE_CHUNK + ":" + chunkIndex + ":" + totalChunks + ":" + data;
    }
    
    public static String createFileEndMessage(String fileName) {
        return FILE_END + ":" + fileName;
    }
    
    public static String createFileErrorMessage(String errorMessage) {
        return FILE_ERROR + ":" + errorMessage;
    }
    
    public void sendFile(File file, SendCallback sendCallback) {
        if (!isValidFile(file)) {
            callback.onTransferError(file.getName(), "Invalid file type or size");
            return;
        }
        
        transferExecutor.submit(() -> {
            try {
                String startMessage = createFileStartMessage(file.getName(), file.length());
                sendCallback.send(EncryptionTool.encrypt(startMessage));
                
                // Read file and send in chunks
                byte[] fileData = Files.readAllBytes(file.toPath());
                int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);
                
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * CHUNK_SIZE;
                    int end = Math.min(start + CHUNK_SIZE, fileData.length);
                    byte[] chunk = new byte[end - start];
                    System.arraycopy(fileData, start, chunk, 0, chunk.length);
                    
                    String encodedChunk = Base64.getEncoder().encodeToString(chunk);
                    String chunkMessage = createFileChunkMessage(i, totalChunks, encodedChunk);
                    sendCallback.send(EncryptionTool.encrypt(chunkMessage));
                    
                    // Report progress
                    int progress = (int) ((i + 1) * 100.0 / totalChunks);
                    callback.onTransferProgress(file.getName(), progress);
                    
                    // Small delay to prevent overwhelming the network
                    Thread.sleep(100);
                }
                
                String endMessage = createFileEndMessage(file.getName());
                sendCallback.send(EncryptionTool.encrypt(endMessage));
                
                callback.onTransferComplete(file.getName(), file);
                
            } catch (Exception e) {
                callback.onTransferError(file.getName(), "Error sending file: " + e.getMessage());
            }
        });
    }
    
    public void processFileMessage(String message) {
        try {
            String[] parts = message.split(":", 4);
            
            if (parts.length < 2) {
                return; // Not a valid file message
            }
            
            String command = parts[0];
            
            if (command.equals(FILE_START)) {
                if (parts.length >= 3) {
                    String fileName = parts[1];
                    long fileSize = Long.parseLong(parts[2]);
                    
                    // Validate file extension
                    String extension = getFileExtension(fileName);
                    if (!isAllowedExtension(extension)) {
                        callback.onTransferError(fileName, "File type not allowed: " + extension);
                        return;
                    }
                    
                    // Initialize file transfer state
                    fileSizes.put(fileName, fileSize);
                    fileChunks.put(fileName, new HashMap<>());
                    int totalChunks = (int) Math.ceil((double) fileSize / CHUNK_SIZE);
                    totalChunksMap.put(fileName, totalChunks);
                    
                    callback.onTransferProgress(fileName, 0);
                }
            } 
            else if (command.equals(FILE_CHUNK)) {
                if (parts.length >= 4) {
                    int chunkIndex = Integer.parseInt(parts[1]);
                    int totalChunks = Integer.parseInt(parts[2]);
                    String data = parts[3];
                    
                    processChunk(chunkIndex, totalChunks, data);
                }
            } 
            else if (command.equals(FILE_END)) {
                String fileName = parts[1];
                completeTransfer(fileName);
            } 
            else if (command.equals(FILE_ERROR)) {
                String errorMessage = parts.length > 1 ? parts[1] : "Unknown error";
                callback.onTransferError("Transfer", errorMessage);
            }
        } catch (Exception e) {
            System.err.println("Error processing file message: " + e.getMessage());
        }
    }
    
    private void processChunk(int chunkIndex, int totalChunks, String data) {
        try {
            // Find the file this chunk belongs to
            String fileName = null;
            for (Map.Entry<String, Integer> entry : totalChunksMap.entrySet()) {
                if (entry.getValue() == totalChunks && !completedFiles.contains(entry.getKey())) {
                    fileName = entry.getKey();
                    break;
                }
            }
            
            if (fileName == null) {
                System.err.println("Could not find active transfer for chunk: " + chunkIndex);
                return;
            }
            
            // Add the chunk
            byte[] chunkData = Base64.getDecoder().decode(data);
            
            Map<Integer, byte[]> chunks = fileChunks.get(fileName);
            chunks.put(chunkIndex, chunkData);
            
            // Check if complete
            if (chunks.size() >= totalChunks) {
                completedFiles.add(fileName);
            }
            
            // Report progress
            int progress = (int) ((chunks.size() * 100.0) / totalChunks);
            callback.onTransferProgress(fileName, progress);
            
        } catch (Exception e) {
            System.err.println("Error processing chunk: " + e.getMessage());
        }
    }
    
    private void completeTransfer(String fileName) {
        try {
            if (!fileChunks.containsKey(fileName)) {
                System.err.println("Could not find active transfer for file: " + fileName);
                return;
            }
            
            // Check if all chunks have been received
            int totalChunks = totalChunksMap.get(fileName);
            Map<Integer, byte[]> chunks = fileChunks.get(fileName);
            
            if (chunks.size() < totalChunks) {
                callback.onTransferError(fileName, "Incomplete file transfer");
                return;
            }
            
            // Assemble file data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i < totalChunks; i++) {
                byte[] chunk = chunks.get(i);
                if (chunk != null) {
                    outputStream.write(chunk, 0, chunk.length);
                }
            }
            byte[] fileData = outputStream.toByteArray();
            
            // Save the file
            String filePath = DOWNLOAD_DIR + File.separator + fileName;
            Files.write(Paths.get(filePath), fileData);
            File file = new File(filePath);

            callback.onTransferComplete(fileName, file);
            
            // Clean up
            fileSizes.remove(fileName);
            fileChunks.remove(fileName);
            totalChunksMap.remove(fileName);
            completedFiles.remove(fileName);
            
        } catch (Exception e) {
            callback.onTransferError(fileName, "Error completing transfer: " + e.getMessage());
        }
    }
    
    // Clean up resources
    public void shutdown() {
        transferExecutor.shutdown();
    }
}