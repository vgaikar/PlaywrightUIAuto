package com.cro.settings;

import java.io.InputStream;
import java.util.Properties;

public class PathConfig {
	private static final Properties properties = new Properties();

	private PathConfig() {
		// intentionally left blank to avoid constructor overloading.
	}

	static {
		try (InputStream inputStream = PathConfig.class.getClassLoader()
				.getResourceAsStream("projectpath.properties")) {
			if (inputStream == null) {
				throw new RuntimeException("projectpath.properties not found.");
			}
			properties.load(inputStream);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load projectpath.properties.", e);
		}
	}

	public static String get(String key) {
		return properties.getProperty(key);
	}
}
