package FileTransfer;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileTransferProtocol {
    // Constants for file transfer protocol messages
    public static final String FILE_START = "FILE_START";
    public static final String FILE_CHUNK = "FILE_CHUNK";
    public static final String FILE_END = "FILE_END";
    public static final String FILE_ERROR = "FILE_ERROR";
    public static final String FILE_REQUEST = "FILE_REQUEST";

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList(".docx", ".pdf", ".jpeg", ".jpg"));
    
    // Maximum file size in bytes (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    // Chunk size for file transfers (64KB)
    public static final int CHUNK_SIZE = 64 * 1024;

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
    
    public static String createFileRequestMessage(String fileName) {
        return FILE_REQUEST + ":" + fileName;
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
}