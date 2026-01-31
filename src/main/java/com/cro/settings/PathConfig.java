/*
* This class will load projectpath.properties to check the project path is configured or not.
*/
 
package com.cro.settings;
 
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
 
public final class PathConfig {
	private static final String DEFAULT_RESOURCE = "project-path.properties";
	private static final String SYS_OVERRIDE    = "paths.file"; // -Dpaths.file=/abs/path.properties
	private PathConfig() {
		//intentionally left blank to avoid constructor overloading
	}
	// one-time, thread-safe init; no locks on reads
	private static final class Holder {
        static final Loaded INSTANCE = loadOnce();
    }
	private record Loaded(Properties props, String source) { 
	}
	private static Loaded loaded() { return Holder.INSTANCE; }
	public static String get(String key) {
        return loaded().props.getProperty(key);
    }
	public static String get(String key, String def) {
        String value = loaded().props.getProperty(key);
        return (value == null || value.trim().isEmpty()) ? def : value.trim();
    }
	/** Diagnostics: where loaded from + values */
    public static void dump(java.util.function.Consumer<String> logger) {
        logger.accept("[PathConfig] Loaded from: " + loaded().source);
        for (Map.Entry<Object,Object> e : loaded().props.entrySet()) {
            logger.accept("  " + e.getKey() + " = " + e.getValue());
        }
    }
// ---------------- internal load-once ----------------
    private static Loaded loadOnce() {
        String override = System.getProperty(SYS_OVERRIDE);
        if (override != null && !override.isBlank()) {
            Path p = Paths.get(override);
            if (!Files.isRegularFile(p)) {
                throw new IllegalStateException("PathConfig override not found: " + p.toAbsolutePath());
            }
            try {
                Properties props = new Properties();
                try (var in = Files.newInputStream(p);
                     Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    props.load(r);
                }
                expand(props);
                return new Loaded(props, "FILE::" + p.toAbsolutePath());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load PathConfig override: " + p, e);
            }
        }
     // classpath fallback
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = PathConfig.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Classpath project path properties not found: " + DEFAULT_RESOURCE);
            }
            Properties props = new Properties();
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(r);
            }
            expand(props);
            return new Loaded(props, "CLASSPATH::" + DEFAULT_RESOURCE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load classpath project paths: " + DEFAULT_RESOURCE, e);
        }
    }
    /** Expand tokens like ${user.dir}. Extend here (e.g., ${buildId}) if needed. */
    private static void expand(Properties p) {
        for (String name : p.stringPropertyNames()) {
            String val = p.getProperty(name);
            if (val != null) {
                val = val.replace("${user.dir}", System.getProperty("user.dir"));
                // this is to read runid from properties file, which used during playwright session strorage folder. 
                val = val.replace("${run.id}", System.getProperty("run.id", "local"));
                p.setProperty(name, val);
            }
        }
    }

}