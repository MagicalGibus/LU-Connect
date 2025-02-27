package FileTransfer;

import Encryption.EncryptionTool;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferManager {
    private static final String DOWNLOAD_DIR = "downloads"; // Download directory for received files
    private final Map<String, FileTransferStatus> activeTransfers = new HashMap<>(); // Tracks ongoing file transfers by filename
    private final ExecutorService transferExecutor = Executors.newFixedThreadPool(3); // To handle file transfers asynchronously
    
    public interface FileTransferCallback {
        void onTransferProgress(String fileName, int progress);
        void onTransferComplete(String fileName, File file);
        void onTransferError(String fileName, String errorMessage);
    }
    
    private FileTransferCallback callback;
    
    public FileTransferManager(FileTransferCallback callback) {
        this.callback = callback;
        
        // Create download directory if it doesn't exist
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
    }
    
    public void sendFile(File file, SendCallback sendCallback) {
        if (!FileTransferProtocol.isValidFile(file)) {
            callback.onTransferError(file.getName(), "Invalid file type or size");
            return;
        }
        
        transferExecutor.submit(() -> {
            try {
                String startMessage = FileTransferProtocol.createFileStartMessage(file.getName(), file.length());
                sendCallback.send(EncryptionTool.encrypt(startMessage));
                
                // Read file and send in chunks
                byte[] fileData = Files.readAllBytes(file.toPath());
                int totalChunks = (int) Math.ceil((double) fileData.length / FileTransferProtocol.CHUNK_SIZE);
                
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * FileTransferProtocol.CHUNK_SIZE;
                    int end = Math.min(start + FileTransferProtocol.CHUNK_SIZE, fileData.length);
                    byte[] chunk = new byte[end - start];
                    System.arraycopy(fileData, start, chunk, 0, chunk.length);
                    
                    String encodedChunk = Base64.getEncoder().encodeToString(chunk);
                    String encryptedChunkMessage = EncryptionTool.encrypt(
                            FileTransferProtocol.createFileChunkMessage(i, totalChunks, encodedChunk));
                    
                    sendCallback.send(encryptedChunkMessage);
                    
                    // Report progress
                    int progress = (int) ((i + 1) * 100.0 / totalChunks);
                    callback.onTransferProgress(file.getName(), progress);
                    
                    // Delay to prevent overwhelming the network
                    Thread.sleep(100);
                }
                
                String endMessage = FileTransferProtocol.createFileEndMessage(file.getName());
                sendCallback.send(EncryptionTool.encrypt(endMessage));
                
                callback.onTransferComplete(file.getName(), file);
                
            } catch (Exception e) {
                callback.onTransferError(file.getName(), "Error sending file: " + e.getMessage());
            }
        });
    }
    
    public void processFileMessage(String message) {
        try {
            String[] parts = message.split(":", 4); // Limit to 4 parts to handle base64 data with colons
            
            if (parts.length < 2) {
                return; // Not a file message
            }
            
            String command = parts[0];
            
            if (command.equals(FileTransferProtocol.FILE_START)) {
                if (parts.length >= 3) {
                    String fileName = parts[1];
                    long fileSize = Long.parseLong(parts[2]);
                    
                    // Validate file extension
                    String extension = FileTransferProtocol.getFileExtension(fileName);
                    if (!FileTransferProtocol.isAllowedExtension(extension)) {
                        callback.onTransferError(fileName, "File type not allowed: " + extension);
                        return;
                    }
                    
                    FileTransferStatus status = new FileTransferStatus(fileName, fileSize);
                    activeTransfers.put(fileName, status);

                    callback.onTransferProgress(fileName, 0);
                }
            } 
            else if (command.equals(FileTransferProtocol.FILE_CHUNK)) {
                if (parts.length >= 4) {
                    int chunkIndex = Integer.parseInt(parts[1]);
                    int totalChunks = Integer.parseInt(parts[2]);
                    String data = parts[3];

                    processFileChunk(chunkIndex, totalChunks, data);
                }
            } 
            else if (command.equals(FileTransferProtocol.FILE_END)) {
                String fileName = parts[1];
                
                completeFileTransfer(fileName);
            } 
            else if (command.equals(FileTransferProtocol.FILE_ERROR)) {
                String errorMessage = parts.length > 1 ? parts[1] : "Unknown error";
                callback.onTransferError("Transfer", errorMessage);
            }
        } catch (Exception e) {
            System.err.println("Error processing file message: " + e.getMessage());
        }
    }
    
    private void processFileChunk(int chunkIndex, int totalChunks, String data) {
        try {
            // Find the active transfer this chunk belongs to
            FileTransferStatus status = null;
            for (FileTransferStatus s : activeTransfers.values()) {
                if (!s.isComplete() && s.getTotalChunks() == totalChunks) {
                    status = s;
                    break;
                }
            }
            
            if (status == null) {
                System.err.println("Could not find active transfer for chunk: " + chunkIndex);
                return;
            }
            
            // Add the chunk
            byte[] chunkData = Base64.getDecoder().decode(data);
            status.addChunk(chunkIndex, chunkData);
            
            int progress = (int) ((status.getReceivedChunks() * 100.0) / totalChunks);
            callback.onTransferProgress(status.getFileName(), progress);
            
        } catch (Exception e) {
            System.err.println("Error processing chunk: " + e.getMessage());
        }
    }
    
    private void completeFileTransfer(String fileName) {
        try {
            FileTransferStatus status = activeTransfers.get(fileName);
            
            if (status == null) {
                System.err.println("Could not find active transfer for file: " + fileName);
                return;
            }
            
            // Check if all chunks have been received
            if (!status.isComplete()) {
                callback.onTransferError(fileName, "Incomplete file transfer");
                return;
            }
            
            // Save the file
            byte[] fileData = status.getFileData();
            String filePath = DOWNLOAD_DIR + File.separator + fileName;
            
            Files.write(Paths.get(filePath), fileData);
            File file = new File(filePath);

            callback.onTransferComplete(fileName, file);
            
            activeTransfers.remove(fileName);
            
        } catch (Exception e) {
            callback.onTransferError(fileName, "Error completing transfer: " + e.getMessage());
        }
    }
    
    public interface SendCallback {
        void send(String message);
    }
    
    public void shutdown() {
        transferExecutor.shutdown();
    }
}