package com.cro.playwright;
 
import com.cro.settings.PathManager;
 
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
 
public final class SessionManager {
 
    private static final ConcurrentHashMap<String, Object> LOCKS = new ConcurrentHashMap<>();
 
    private SessionManager() {}
 
    /**
     * Returns storageState path if session already exists.
     * Ensures only ONE thread creates session per user.
     */
    public static Path getOrCreateSession(String role,Runnable loginFlow) {
        Path sessionFile = PathManager.sessionDir().resolve(role + ".json");
        Object lock = LOCKS.computeIfAbsent(role, u -> new Object());
        synchronized (lock) {
            try {
                if (Files.exists(sessionFile)) {
                    return sessionFile; // ✅ reuse
                }
                // 🔐 No context/page creation here
                // BrowserManager will create context & page
                loginFlow.run(); // login must save storageState externally
 
                return sessionFile;
 
            } catch (Exception e) {
            	throw new RuntimeException("Session creation failed for role: " + role, e);
            }
        }
    }
}