/*

* This class will read properties file. This class is only for Classpath configs (under src/test/resources)

* I uses fallback mechanism as mentioned below:

* 1. If maven runtime property is passed it will pick it

* 2. If not passed it will go to default properties section

* 3. Also some property can be directly passed from maven like URL:http://google.com

* Classpath-first environment properties loader:

*   Precedence: -DconfigUrl / CONFIG_URL  -> classpath: config/config-<env>.properties

*

* Provides cached variants for parallel runs.

*/
 
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

import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;
 
public final class PropertiesLoader {

	private static final String ENV_FILE_TEMPLATE = "config/config-%s.properties";

    private static final String TEST_DEFAULT_ENV_FILE = "config/config-test-default.properties";

    private static final String MAIN_DEFAULT_ENV_FILE = "config/config-app-default.properties";

// Cache: key is "URL::<spec>" or "CLASSPATH::<env>"

    private static final ConcurrentHashMap<String, Properties> CACHE = new ConcurrentHashMap<>();

    private PropertiesLoader() {

    	//intentionally left blank to avoid constructor overloading

    }

    // -------- Public API (non-cached) --------
 
    public static Properties load() throws IOException {

        String env = normalizeEnv(resolveEnvStrict());

        return loadForEnv(env);

    }

    public static Properties loadForEnv(String env) throws IOException {

    	String normalizedEnv = normalizeEnv(env);

        if (!isNonBlank(normalizedEnv)) {

            throw new IllegalArgumentException("env is required (dev/val/uat etc.).");

        }

        String urlSpec = firstNonBlank(System.getProperty("configUrl"), System.getenv("CONFIG_URL"));

        if (isNonBlank(urlSpec)) {

            return loadFromUrlOrFileSpec(urlSpec);

        }

        String resourcePath = String.format(ENV_FILE_TEMPLATE, normalizedEnv);

        try (InputStream in = cl().getResourceAsStream(resourcePath)) {

            if (in != null) {

                Properties prop = new Properties();

                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                    prop.load(r);

                }

                return prop;

            }

        }

        throw new FileNotFoundException("Config not found for env=" + normalizedEnv +

                ". Tried classpath: " + resourcePath +

                (isNonBlank(urlSpec) ? " and URL=" + urlSpec : ""));

    }

// -------- Public API (cached) --------
 
    public static Properties loadCached() throws IOException {

        String env = normalizeEnv(resolveEnvStrict());

        return loadForEnvCached(env);

    }

    public static Properties loadForEnvCached(String env) throws IOException {

        String normalizedEnv = normalizeEnv(env);

        if (!isNonBlank(normalizedEnv)) {

            throw new IllegalArgumentException("env is required (dev/val/uat etc.).");

        }

        String key = cacheKeyForCurrentSource(normalizedEnv);

        try {

            return CACHE.computeIfAbsent(key, k -> {

                try { return loadForEnv(normalizedEnv); }

                catch (IOException e) { throw new UncheckedIOException(e); }

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

        String envProp = System.getProperty("env");

        if (isNonBlank(envProp)) return envProp.trim();
 
        String envVar = System.getenv("ENV");

        if (isNonBlank(envVar)) return envVar.trim();
 
        String test = readEnvFromClasspath(TEST_DEFAULT_ENV_FILE);

        if (isNonBlank(test)) return test.trim();
 
        String main = readEnvFromClasspath(MAIN_DEFAULT_ENV_FILE);

        if (isNonBlank(main)) return main.trim();
 
        throw new IllegalArgumentException(

            "Environment not resolved. Expected one of:\n" +

            "  - JVM property: -Denv=<dev|val|uat>\n" +

            "  - Environment variable: ENV=<dev|val|uat>\n" +

            "  - Classpath default (test): " + TEST_DEFAULT_ENV_FILE + " with key 'env' or 'ENV'\n" +

            "  - Classpath default (main): " + MAIN_DEFAULT_ENV_FILE + " with key 'env' or 'ENV'\n"

        );

    }

    private static String readEnvFromClasspath(String resourcePath) throws IOException {

        try (InputStream in = cl().getResourceAsStream(resourcePath)) {

            if (in == null) return null;

            Properties defaults = new Properties();

            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                defaults.load(r);

            }

            for (String name : defaults.stringPropertyNames()) {

                if ("env".equalsIgnoreCase(name)) {

                    String v = defaults.getProperty(name);

                    if (isNonBlank(v)) return v.trim();

                }

            }

            return null;

        }

    }

// -------- URL/file loader --------
 
    private static Properties loadFromUrlOrFileSpec(String spec) throws IOException {

        try {

            URL url = URI.create(spec).toURL(); // http, https, file

            try (InputStream in = url.openStream();

                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                Properties p = new Properties();

                p.load(r);

                return p;

            }

        } catch (IllegalArgumentException e) {

            try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(spec));

                 Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

                Properties p = new Properties();

                p.load(r);

                return p;

            } catch (IOException io) {

                throw new FileNotFoundException("Config URL/path not accessible: " + spec + " (" + io.getMessage() + ")");

            }

        }

    }

// -------- Helpers --------
 
    private static String cacheKeyForCurrentSource(String normalizedEnv) {

        String urlSpec = firstNonBlank(System.getProperty("configUrl"), System.getenv("CONFIG_URL"));

        return (isNonBlank(urlSpec)) ? ("URL::" + urlSpec) : ("CLASSPATH::" + normalizedEnv);

    }
 
    private static ClassLoader cl() {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        return (cl != null) ? cl : PropertiesLoader.class.getClassLoader();

    }
 
    private static boolean isNonBlank(String s) {

        return s != null && !s.trim().isEmpty();

    }
 
    private static String firstNonBlank(String... values) {

        for (String v : values) if (isNonBlank(v)) return v;

        return null;

    }
 
    private static String normalizeEnv(String env) {

        return env == null ? null : env.trim().toLowerCase();

    }    

}

 