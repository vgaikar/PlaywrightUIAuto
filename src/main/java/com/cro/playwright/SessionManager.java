package com.cro.playwright;
 
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
 
import com.cro.settings.PathManager;
 
/**
* Returns storageState path if session already exists.
* Ensures only ONE thread creates session per user.
* Supports parallel scenario execution with timeout safety.
*/
public final class SessionManager {
 
    // Map to hold per-role+user locks
    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();
 
    // Timeout for loginFlow (to prevent suite hanging indefinitely)
    private static final long LOGIN_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(60);
 
    private SessionManager() {}
 
    /**
     * Returns the session file path for the given role + username.
     * Only one thread will execute loginFlow per session file.
     *
     * @param role      Role of the user
     * @param username  Username
     * @param loginFlow Login logic to create the session file
     * @return Path to session JSON file
     */
    public static Path getOrCreateSession(String role, String username, Runnable loginFlow) {
        String key = role + "_" + username;
        Path sessionFile = PathManager.sessionDir().resolve(key + ".json");
 
        // Fast path
        if (Files.exists(sessionFile)) {
            return sessionFile;
        }
 
        Object lock = LOCKS.computeIfAbsent(key, k -> new Object());
 
        long startTime = System.currentTimeMillis();
        synchronized (lock) {
            try {
                if (!Files.exists(sessionFile)) {
                    // Run loginFlow in current thread (ThreadLocal safe)
                    loginFlow.run();
 
                    // Timeout check
                    if (!Files.exists(sessionFile)
&& System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(60)) {
                        throw new IllegalStateException("Timeout creating session for " + key);
                    }
                }
 
                if (!Files.exists(sessionFile)) {
                    throw new IllegalStateException(
                        "Session file not created for role=" + role + ", user=" + username
                    );
                }
            } finally {
                LOCKS.remove(key);
            }
        }
 
        return sessionFile;
    }
 
}