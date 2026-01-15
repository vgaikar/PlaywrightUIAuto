package com.cro.settings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathManager {
	private static final Path BASE_DIR = Paths.get(System.getProperty("user.dir"));

	private PathManager() {
		// intentionally left blank to avoid constructor overloading.
	}
	
	public static Path configDir() {
		return BASE_DIR.resolve(PathConfig.get("config.dir"));
	}

	public static Path reportDir() {
		return BASE_DIR.resolve(PathConfig.get("report.dir"));
	}

	public static Path logDir() {
		return BASE_DIR.resolve(PathConfig.get("log.dir"));
	}

	public static Path screenshotDir() {
		return BASE_DIR.resolve(PathConfig.get("screenshot.dir"));
	}

	public static Path videoDir() {
		return BASE_DIR.resolve(PathConfig.get("video.dir"));
	}

	public static Path downloadDir() {
		return BASE_DIR.resolve(PathConfig.get("download.dir"));
	}

	// Create directories if missing
	public static void createRequiredDirs() {
		try {
			Files.createDirectories(reportDir());
			Files.createDirectories(logDir());
			Files.createDirectories(screenshotDir());
			Files.createDirectories(videoDir());
			Files.createDirectories(downloadDir());

		} catch (Exception e) {
			throw new RuntimeException("ERROR: Failed to create directories", e);

		}
	}
}
