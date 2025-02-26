package Server;

import java.util.concurrent.Semaphore;

public class ConnectionSemaphore {
    private static final int MAX_CONNECTIONS = 3;
    private final Semaphore semaphore;
    
    public ConnectionSemaphore() {
        // Initialize the semaphore with MAX_CONNECTIONS, connections granted in the order that they were requested
        this.semaphore = new Semaphore(MAX_CONNECTIONS, true);
    }
    
    public boolean tryAcquireConnection() {
        return semaphore.tryAcquire();
    }
    
    public void acquireConnection() throws InterruptedException {
        semaphore.acquire();
    }

    public void releaseConnection() {
        semaphore.release();
    }
    
    public int getAvailableConnections() {
        return semaphore.availablePermits();
    }
    
    public int getQueueLength() {
        return semaphore.getQueueLength();
    }
}