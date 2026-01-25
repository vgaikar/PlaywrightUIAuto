package com.cro.settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PropertiesLoader {
    private static final String ENV_FILE_TEMPLATE = "config/config-%s.properties";
    private static final String TEST_DEFAULT_ENV_FILE = "config/config-test-default.properties";
    private static final String MAIN_DEFAULT_ENV_FILE = "config/config-app-default.properties";

    // Cache: key is "URL::<spec>" or "CLASSPATH::<env>" or "CLASSPATH::DEFAULTS"
    private static final ConcurrentHashMap<String, Properties> CACHE = new ConcurrentHashMap<>();

    // Browser-related constants and default
    private static final String SYS_PROP_BROWSER = "browser";
    private static final String ENVVAR_BROWSER = "BROWSER";
    private static final String PROPKEY_BROWSER = "browser";
    private static final String DEFAULT_BROWSER = "chrome";  //fallabck logic --> If no browser is found in properties, your loader silently falls back to chrome.

    private PropertiesLoader() {
        // intentionally left blank to avoid constructor overloading
    }

    // ===== NEW ===== Case-insensitive Properties =====
    /**
     * A Properties variant that stores keys in lowercase (Locale.ROOT)
     * so that all key lookups become case-insensitive.
     */
    private static final class CaseInsensitiveProperties extends Properties {
        private static final long serialVersionUID = 1L;

        private static String norm(Object key) {
            if (key == null) return null;
							
            if (!(key instanceof String)) return String.valueOf(key);
            return ((String) key).toLowerCase(Locale.ROOT);
        }

        @Override
        public synchronized Object put(Object key, Object value) {
            return super.put(norm(key), value);
        }

        @Override
        public synchronized void putAll(Map<?, ?> t) {
            for (Map.Entry<?, ?> e : t.entrySet()) {
                super.put(norm(e.getKey()), e.getValue());
            }
								
        }

        @Override
        public String getProperty(String key) {
            return super.getProperty(norm(key));
        }

        @Override
        public synchronized Object get(Object key) {
            return super.get(norm(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(norm(key));
        }
        
        @Override
        public synchronized Object setProperty(String key, String value) {
            return super.setProperty(norm(key), value);
        }

        // stringPropertyNames() and propertyNames() will reflect normalized keys via super
        @Override
        public Set<String> stringPropertyNames() {
            return super.stringPropertyNames();								  
        }
    }

    // -------- Public API (non-cached) --------
    public static Properties load() throws IOException {
        // Highest precedence: external URL/file override
        String urlSpec = firstNonBlank(getSystemPropertyIgnoreCase("configUrl"), getEnvVarIgnoreCase("CONFIG_URL"));
        if (isNonBlank(urlSpec)) {
            return loadFromUrlOrFileSpec(urlSpec);
        }

        // 1) JVM property: -Denv=...
        String envProp = getSystemPropertyIgnoreCase("env"); // case-insensitive
        if (isNonBlank(envProp)) {
            return loadForEnv(envProp);
        }

        // 2) Environment variable: ENV=...
        String envVar = getEnvVarIgnoreCase("ENV");
        if (isNonBlank(envVar)) {
            return loadForEnv(envVar);
        }

        // 3) Fallbacks (in exact order), first resolve env if not found comeout (check in app-default), if found read URL, URL empty throw error
        Properties testDefaultProperties = tryLoadFromClasspath(TEST_DEFAULT_ENV_FILE);
        if (testDefaultProperties != null) {
            String fallbackEnv = testDefaultProperties.getProperty("env");           
            if (isNonBlank(fallbackEnv)) {
            	System.setProperty("env", normalizeEnv(fallbackEnv));
            	String baseUrl = testDefaultProperties.getProperty("base.url");
            	if (!isNonBlank(baseUrl)) {
            		throw new IllegalStateException("Missing required property 'base.url' in " + TEST_DEFAULT_ENV_FILE);
            	}
            	return testDefaultProperties; // only if env exists
            }
        }

        Properties mainDefaultProperties = tryLoadFromClasspath(MAIN_DEFAULT_ENV_FILE);
        if (mainDefaultProperties != null) {
            String fallbackEnv = mainDefaultProperties.getProperty("env");            
            if (isNonBlank(fallbackEnv)) {
            	System.setProperty("env", normalizeEnv(fallbackEnv));
            	String baseUrl = mainDefaultProperties.getProperty("base.url");
                if (!isNonBlank(baseUrl)) {
                    throw new IllegalStateException("Missing required property 'base.url' in " + MAIN_DEFAULT_ENV_FILE);
                }
                return mainDefaultProperties; //last place to resolve env before throwing error
            }
            
        }
        
        // 4) If we get here, env was not resolved anywhere → fail fast
        throw new FileNotFoundException("Environment not supplied and default configs not found or missing 'env'. Tried: "
                + TEST_DEFAULT_ENV_FILE + ", " + MAIN_DEFAULT_ENV_FILE);
    }

    public static Properties loadForEnv(String env) throws IOException {
        String normalizedEnv = normalizeEnv(env);
        if (!isNonBlank(normalizedEnv)) {
            throw new IllegalArgumentException("env is required (dev/val/uat etc.).");
        }
        String urlSpec = firstNonBlank(getSystemPropertyIgnoreCase("configUrl"), getEnvVarIgnoreCase("CONFIG_URL"));
        if (isNonBlank(urlSpec)) {
            return loadFromUrlOrFileSpec(urlSpec);
        }
        String resourcePath = String.format(ENV_FILE_TEMPLATE, normalizedEnv);
        try (InputStream in = cl().getResourceAsStream(resourcePath)) {
            if (in != null) {
                Properties prop = new CaseInsensitiveProperties(); // <<< key normalization here
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    prop.load(r);
                }
                return prop;
            }
        }
        throw new FileNotFoundException("Config not found for env=" + normalizedEnv + ". Tried classpath: "
                + resourcePath + (isNonBlank(urlSpec) ? " and URL=" + urlSpec : ""));
    }

    // -------- Public API (cached) --------
    public static Properties loadCached() throws IOException {
        // Highest precedence: external URL/file override
        String urlSpec = firstNonBlank(getSystemPropertyIgnoreCase("configUrl"), getEnvVarIgnoreCase("CONFIG_URL"));
        if (isNonBlank(urlSpec)) {
            String key = "URL::" + urlSpec;
            try {
                return CACHE.computeIfAbsent(key, k -> {
                    try {
                        return loadFromUrlOrFileSpec(urlSpec);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException uioe) {
                throw uioe.getCause();
            }
        }

        // 1) JVM property: -Denv=...
        String envProp = getSystemPropertyIgnoreCase("env"); // case-insensitive
        if (isNonBlank(envProp)) {
            return loadForEnvCached(envProp);
        }

        // 2) Environment variable: ENV=...
        String envVar = getEnvVarIgnoreCase("ENV");
        if (isNonBlank(envVar)) {
            return loadForEnvCached(envVar);
        }

        // 3) Fallbacks (same order), cached with stable keys
        final String defaultCacheKey = "CLASSPATH::DEFAULTS";
        try {
            return CACHE.computeIfAbsent(defaultCacheKey, k -> {
                try {
                    // Try test-default first
                    Properties testCachedDefaultProperties = tryLoadFromClasspath(TEST_DEFAULT_ENV_FILE);
                    Properties mainCachedDefaultProperties = tryLoadFromClasspath(MAIN_DEFAULT_ENV_FILE);

                    if (testCachedDefaultProperties != null) {
                        String fallbackEnv = testCachedDefaultProperties.getProperty("env");
                        String baseUrl = testCachedDefaultProperties.getProperty("base.url");
                        if (isNonBlank(fallbackEnv)) {
                            System.setProperty("env", normalizeEnv(fallbackEnv));

                            if (!isNonBlank(baseUrl)) {
                                throw new IllegalStateException(
                                    "Missing required property 'base.url' in " + TEST_DEFAULT_ENV_FILE
                                );
                            }

                            System.out.println("[PropertiesLoader] Loaded properties from fallback: " 
                                    + TEST_DEFAULT_ENV_FILE + " (env=" + fallbackEnv 
                                    + ", base.url=" + baseUrl + ")");
                            return testCachedDefaultProperties;
                        }
                    }

                    // Then try main-default
                    if (mainCachedDefaultProperties != null) {
                        String fallbackEnv = mainCachedDefaultProperties.getProperty("env");
                        String baseUrl = mainCachedDefaultProperties.getProperty("base.url");
                        if (isNonBlank(fallbackEnv)) {
                            System.setProperty("env", normalizeEnv(fallbackEnv));

                            if (!isNonBlank(baseUrl)) {
                                throw new IllegalStateException(
                                    "Missing required property 'base.url' in " + MAIN_DEFAULT_ENV_FILE
                                );
                            }

                            System.out.println("[PropertiesLoader] Loaded properties from fallback: " 
                                    + MAIN_DEFAULT_ENV_FILE + " (env=" + fallbackEnv 
                                    + ", base.url=" + baseUrl + ")");
                            return mainCachedDefaultProperties;
                        }
                    }

                    // If neither fallback works, throw
                    String testMsg = (testCachedDefaultProperties == null) ? " (not found)" : " (found but missing 'env/base.url')";
                    String mainMsg = (mainCachedDefaultProperties == null) ? " (not found)" : " (found but missing 'env/base.url')";

                    throw new FileNotFoundException(
                        "Unable to resolve environment configuration!\n" +
                        "  1) No '-Denv' JVM property was set.\n" +
                        "  2) No 'ENV' environment variable was set.\n" +
                        "  3) No valid external config URL/path found: "
                            + firstNonBlank(getSystemPropertyIgnoreCase("configUrl"), getEnvVarIgnoreCase("CONFIG_URL")) + "\n" +
                        "  4) Default fallback properties either missing or invalid:\n" +
                            "     - " + TEST_DEFAULT_ENV_FILE + testMsg + "\n" +
                            "     - " + MAIN_DEFAULT_ENV_FILE + mainMsg + "\n" +
                        "Please ensure one of the above sources provides a valid 'env' and 'base.url'."
                    );

                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException uioe) {
            throw uioe.getCause();                                            
        }
    }
    public static Properties loadForEnvCached(String env) throws IOException {
        String normalizedEnv = normalizeEnv(env);
        if (!isNonBlank(normalizedEnv)) {
            throw new IllegalArgumentException("env is required (dev/val/uat etc.).");
        }
        String key = cacheKeyForCurrentSource(normalizedEnv);
        try {
            return CACHE.computeIfAbsent(key, k -> {
                try {
                    return loadForEnv(normalizedEnv);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException uioe) {
            throw uioe.getCause();
        }
    }

    public static String effectiveEnv() throws IOException {
        return normalizeEnv(resolveEnvStrict());
    }
		
    public static void resetCache() {
        CACHE.clear();
    }

    // -------- Env resolution (STRICT) --------

    private static String resolveEnvStrict() throws IOException {
        // 1) JVM property: -Denv=...
        String envProp = getSystemPropertyIgnoreCase("env"); // case-insensitive
        if (isNonBlank(envProp)) return envProp.trim();

        String envVar = getEnvVarIgnoreCase("ENV");
        if (isNonBlank(envVar)) return envVar.trim();																					   
  

        String test = readEnvFromClasspath(TEST_DEFAULT_ENV_FILE);
        if (isNonBlank(test)) return test.trim();	

        String main = readEnvFromClasspath(MAIN_DEFAULT_ENV_FILE);
																						  
        if (isNonBlank(main)) return main.trim();

        throw new IllegalArgumentException(errorLocation() + "Environment not resolved. Expected one of:\n"
                + "  - JVM property: -Denv=<dev|val|uat>\n"
                + "  - Environment variable: ENV=<dev|val|uat>\n"
                + "  - Classpath default (test): " + TEST_DEFAULT_ENV_FILE + " with key 'env'\n"
                + "  - Classpath default (main): " + MAIN_DEFAULT_ENV_FILE + " with key 'env'\n");
    }

    private static String readEnvFromClasspath(String resourcePath) throws IOException {
        try (InputStream in = cl().getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            Properties defaults = new CaseInsensitiveProperties();
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                defaults.load(r);
            }
            // Keys are already case-insensitive
            return defaults.getProperty("env");
        }
    }

    // -------- URL/file loader --------
    private static Properties loadFromUrlOrFileSpec(String spec) throws IOException {
        try {
            URL url = URI.create(spec).toURL(); // http, https, file
            try (InputStream in = url.openStream();
                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Properties p = new CaseInsensitiveProperties(); // <<< key normalization here
                p.load(r);
                return p;
            }
        } catch (IllegalArgumentException e) {
            try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(spec));
                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Properties p = new CaseInsensitiveProperties(); // <<< key normalization here
                p.load(r);
                return p;
            } catch (IOException io) {
                throw new FileNotFoundException("Config URL/path not accessible: " + spec + " (" + io.getMessage() + ")");
            }
        }
    }

    // -------- Helpers --------							

    /**
     * Returns a prefix like "[ClassName.methodName] " for use in exception messages.
     */
    private static String errorLocation() {
        // 0 = getStackTrace(), 1 = this method, 2 = method that called errorLocation()
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        String fullClass = caller.getClassName();
        String simpleClass = fullClass.substring(fullClass.lastIndexOf('.') + 1);
        String method = caller.getMethodName();
        return "[" + simpleClass + "." + method + "] ";
    }
					
    private static String cacheKeyForCurrentSource(String normalizedEnv) {
        String urlSpec = firstNonBlank(getSystemPropertyIgnoreCase("configUrl"), getEnvVarIgnoreCase("CONFIG_URL"));
        return (isNonBlank(urlSpec)) ? ("URL::" + urlSpec) : ("CLASSPATH::" + normalizedEnv);
    }

    private static ClassLoader cl() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return (cl != null) ? cl : PropertiesLoader.class.getClassLoader();
    }

											 
    private static boolean isNonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // Unified "first non-blank" finder
    private static String firstNonBlank(String... values) {
        for (String value : values)
            if (isNonBlank(value))
                return value;
        return null;
    }

    // Normalize env values consistently
    private static String normalizeEnv(String env) {
        return env == null ? null : env.trim().toLowerCase(Locale.ROOT);
    }
												   

    private static Properties tryLoadFromClasspath(String resourcePath) throws IOException {
        try (InputStream in = cl().getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            Properties prop = new CaseInsensitiveProperties(); // <<< key normalization here
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                prop.load(r);
            }
            return prop;
        }
    }
    
    // Lightweight browser enum + normalizer
    public enum Browser {
        CHROME, FIREFOX, EDGE, SAFARI;
        static Browser fromStringStrict(String raw) {
            String s = (raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
            switch (s) {
                case "chrome":
                case "chromium":
                case "gc":
                case "googlechrome":
                    return CHROME;
                case "firefox":
                case "ff":
                case "mozilla":
                    return FIREFOX;
                case "edge":
                case "msedge":
                    return EDGE;
                case "safari":
                    return SAFARI;
                default:
                    throw new IllegalArgumentException(
                        "Unsupported browser '" + raw + "'. Allowed: chrome, firefox, edge, safari");
            }
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    // Collapse duplicate code in effectiveBrowser() / effectiveBrowserCached()
    private static String resolveBrowser(Properties props) {
        String fromProps = props.getProperty(PROPKEY_BROWSER); // case-insensitive key
        if (isNonBlank(fromProps)) {
            return Browser.fromStringStrict(fromProps).toString();
        }
        return Browser.fromStringStrict(DEFAULT_BROWSER).toString();
    }

    public static String effectiveBrowser() throws IOException {
        String sys = getSystemPropertyIgnoreCase(SYS_PROP_BROWSER); // case-insensitive system property
        if (isNonBlank(sys)) return Browser.fromStringStrict(sys).toString();

        String env = getEnvVarIgnoreCase(ENVVAR_BROWSER); // case-insensitive env var
        if (isNonBlank(env)) return Browser.fromStringStrict(env).toString();

        return resolveBrowser(load());
    }

    public static String effectiveBrowserCached() throws IOException {
        String sys = getSystemPropertyIgnoreCase(SYS_PROP_BROWSER);
        if (isNonBlank(sys)) return Browser.fromStringStrict(sys).toString();

        String env = getEnvVarIgnoreCase(ENVVAR_BROWSER);
        if (isNonBlank(env)) return Browser.fromStringStrict(env).toString();

        return resolveBrowser(loadCached());
    }

    // Optional: enum-returning helpers for stronger typing
    public static Browser effectiveBrowserAsEnum() throws IOException {
        return Browser.fromStringStrict(effectiveBrowser());
    }

    public static Browser effectiveBrowserAsEnumCached() throws IOException {
        return Browser.fromStringStrict(effectiveBrowserCached());
    }

    // ===== NEW ===== Generic, reusable case-insensitive lookups for System props & env vars

    /**
     * Case-insensitive lookup of a JVM system property name.
     * Supports -Denv=..., -DENV=..., -DeNv=..., etc.
     */
    private static String getSystemPropertyIgnoreCase(String key) {
        if (!isNonBlank(key)) return null;
        // Fast path: common exact
        String direct = System.getProperty(key);
        if (isNonBlank(direct)) return direct;

        // Fallback: scan for any key equalsIgnoreCase
        for (String name : System.getProperties().stringPropertyNames()) {
            if (key.equalsIgnoreCase(name)) {
                String val = System.getProperty(name);
                if (isNonBlank(val)) return val;
            }
        }
        return null;
    }

    /**
     * Case-insensitive lookup of an environment variable name.
     * Useful across platforms; on Linux env names are case-sensitive,
     * but we opt-in to "best effort" scanning for consistency.
     */
    private static String getEnvVarIgnoreCase(String key) {
        if (!isNonBlank(key)) return null;
        String direct = System.getenv(key);
        if (isNonBlank(direct)) return direct;

        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            if (key.equalsIgnoreCase(e.getKey())) {
                if (isNonBlank(e.getValue())) return e.getValue();
            }
        }
        return null;
    }
}
