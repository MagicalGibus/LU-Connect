package FileTransfer;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class FileTransferStatus {
    private final String fileName;
    private final long fileSize;
    private final Map<Integer, byte[]> chunks;
    private int totalChunks;
    private boolean complete;
    
    public FileTransferStatus(String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.chunks = new HashMap<>();
        this.complete = false;
        
        // Calculate expected number of chunks
        this.totalChunks = (int) Math.ceil((double) fileSize / FileTransferProtocol.CHUNK_SIZE);
    }
    
    public synchronized void addChunk(int index, byte[] data) {
        chunks.put(index, data);
        checkComplete();
    }
    
    private void checkComplete() {
        if (chunks.size() >= totalChunks) {
            complete = true;
        }
    }
    
    public synchronized byte[] getFileData() {
        if (!complete) {
            return null;
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunk = chunks.get(i);
            if (chunk != null) {
                outputStream.write(chunk, 0, chunk.length);
            }
        }
        
        return outputStream.toByteArray();
    }
    
    public int getReceivedChunks() {
        return chunks.size();
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public boolean isComplete() {
        return complete;
    }

    public String getFileName() {
        return fileName;
    }
    
    public long getFileSize() {
        return fileSize;
    }
}