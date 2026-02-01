/*
* This class is only for real filesystem outputs (reports, logs, screenshots, video, downloads).
*/
 
package com.cro.settings;
 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
public final class PathManager {
	private PathManager() {
		//intentionally left blank to avoid constructor overloading
	}
	private static final class Holder {
        static final ResolvedPaths INSTANCE = resolveAll();
    }
	 private record ResolvedPaths(
		        Path baseDir,
		        Path reportDir,
		        Path logDir,
		        Path screenshotDir,
		        Path videoDir,
		        Path downloadDir,
		        Path sessionDir
		    ) { }
	 /** Reads base.dir from PathConfig (defaults to ${user.dir}) and normalizes to absolute Path. */
	    private static Path baseDir() {
	        String baseSpec = PathConfig.get("base.dir", System.getProperty("user.dir"));
	        return Paths.get(baseSpec).toAbsolutePath().normalize();
	    }
	    public static Path baseDirPath()   { return Holder.INSTANCE.baseDir; }
	    public static Path reportDir()     { return Holder.INSTANCE.reportDir; }
	    public static Path logDir()        { return Holder.INSTANCE.logDir; }
	    public static Path screenshotDir() { return Holder.INSTANCE.screenshotDir; }
	    public static Path videoDir()      { return Holder.INSTANCE.videoDir; }
	    public static Path downloadDir()   { return Holder.INSTANCE.downloadDir; }
	    public static Path sessionDir()    { return Holder.INSTANCE.sessionDir; }
	    /** Ensure output folders exist; safe to call multiple times and in parallel. */
	    public static void createRequiredDirs() {
	        try {
	            Files.createDirectories(reportDir());
	            Files.createDirectories(logDir());
	            Files.createDirectories(screenshotDir());
	            Files.createDirectories(videoDir());
	            Files.createDirectories(downloadDir());
	            Files.createDirectories(sessionDir());
	        } catch (IOException e) {
	            throw new RuntimeException("Error: Failed to create one or more directories", e);
	        }
	    }
	 // --------- internal ---------
 
	    private static ResolvedPaths resolveAll() {
	        Path base        = baseDir();
	        Path report      = resolveUnder(base, PathConfig.get("report.dir", "reports"));
	        Path log         = resolveUnder(base, PathConfig.get("log.dir", "logs"));
	        Path screenshots = resolveUnder(base, PathConfig.get("screenshot.dir", "extent-reports/screenshots"));
	        Path video       = resolveUnder(base, PathConfig.get("video.dir", "extent-reports/screenshots"));
	        Path download    = resolveUnder(base, PathConfig.get("download.dir", "downloads"));
	        Path session = resolveUnder(base,PathConfig.get("session.dir", "sessions/${run.id}"));
	        return new ResolvedPaths(base, report, log, screenshots, video, download, session);
	    }
 
	    private static Path resolveUnder(Path base, String spec) {
	        Path p = Paths.get(spec);
	        return p.isAbsolute() ? p.normalize() : base.resolve(p).toAbsolutePath().normalize();
	    }
}