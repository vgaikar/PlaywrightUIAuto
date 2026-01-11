/*
* This class will read properties file. I uses fallback mechanism as mentioned below:
* 1. If maven runtime property is passed it will pick it
* 2. If not passed it will go to default properties section
* 3. Also some property can be directly passed from maven like URL:http://abc.com
*/
 
package com.cro.settings;
 
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
 
public final class PropertiesLoader {
	// Flat filenames (no env subfolders)
    private static final String ENV_FILE_TEMPLATE = "config/config-%s.properties";
 
    // Default env discovery files (classpath)
    private static final String TEST_DEFAULT_ENV_FILE = "config/config-test-default.properties";
    private static final String MAIN_DEFAULT_ENV_FILE = "config/config-app-default.properties";
 
    // Accept either 'env' or 'ENV' as the key in default files
    private static final String[] ENV_KEYS = new String[] { "env", "ENV" };
 
    private PropertiesLoader() {
    	
    }
    /**
     * Entry point: resolve env strictly (no dev fallback), then load properties.
     */
    public static Properties load() throws IOException {
        String env = resolveEnvStrict();
        return loadForEnv(env);
    }
    
    /**
     * Load properties for a given env (assumes env already resolved).
     */
    public static Properties loadForEnv(String env) throws IOException {
    	if (env == null || env.trim().isEmpty()) {
            throw new IllegalArgumentException("env is required (e.g., dev/val/uat) — no default will be used");
        }
        final String normalizedEnv = env.trim();
        
     // 1) Allow direct URL/file override — highest priority
        String urlSpec = firstNonBlank(
                System.getProperty("configUrl"),
                System.getenv("CONFIG_URL")
        );
        if (isNonBlank(urlSpec)) {
            return loadFromUrlOrFileSpec(urlSpec);
        }
        
     // 2) Classpath (TEST precedes MAIN automatically during test runs)
        String resourcePath = String.format(ENV_FILE_TEMPLATE, normalizedEnv);
 
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = PropertiesLoader.class.getClassLoader();
 
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                return p;
            }
        }
 
        // Not found — list attempted location(s)
        throw new FileNotFoundException(
                "Config not found for env=" + normalizedEnv +
                ". Tried classpath: " + resourcePath +
                (isNonBlank(urlSpec) ? " and URL=" + urlSpec : "")
        );
    	
    }
    
// ---------- Env resolution (STRICT: NO default 'dev') ----------
    /**
     * Resolve env strictly:
     *  1) -Denv
     *  2) ENV env var
     *  3) classpath test default file
     *  4) classpath main/app default file
     *  5) else throw
     * @throws IOException
     */
    private static String resolveEnvStrict() throws IOException {
    	// 1) JVM property
        String envProp = System.getProperty("env");
        if (isNonBlank(envProp)) return envProp.trim();
		
     // 2) ENV variable
        String envVar = System.getenv("ENV");
        if (isNonBlank(envVar)) return envVar.trim();
        
     // 3) Default from test file
        String envFromTestDefault = readEnvFromClasspath(TEST_DEFAULT_ENV_FILE);
        if (isNonBlank(envFromTestDefault)) return envFromTestDefault.trim();
 
        // 4) Default from main/app file
        String envFromMainDefault = readEnvFromClasspath(MAIN_DEFAULT_ENV_FILE);
        if (isNonBlank(envFromMainDefault)) return envFromMainDefault.trim();
        
     // 5) Fail (no dev fallback)
        throw new IllegalArgumentException(
                "Environment not resolved. Expected one of:\n" +
                "  - JVM property: -Denv=<dev|val|uat>\n" +
                "  - Environment variable: ENV=<dev|val|uat>\n" +
                "  - Classpath default (test): " + TEST_DEFAULT_ENV_FILE + " with key 'env' or 'ENV'\n" +
                "  - Classpath default (main): " + MAIN_DEFAULT_ENV_FILE + " with key 'env' or 'ENV'\n"
        );        
       
	}
    
    /**
     * Read env value from a classpath properties file using keys 'env' or 'ENV'.
     *
     * @param resourcePath classpath resource path
     * @return env value if found and non-blank, else null
     */    
	
    private static String readEnvFromClasspath(String resourcePath) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) cl = PropertiesLoader.class.getClassLoader();
 
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            Properties defaults = new Properties();
            defaults.load(in);
            for (String key : ENV_KEYS) {
                String val = defaults.getProperty(key);
                if (isNonBlank(val)) return val.trim();
            }
            return null;
        }
    }
    
// ---------- URL (and file spec) loader ----------
    /**
     * Load properties from a URL spec. Supports http(s):// and file:///.
     * If a plain filesystem path is provided, attempts to read it directly as a file.
     */
    private static Properties loadFromUrlOrFileSpec(String spec) throws IOException {
    	try {
            URL url = new URL(spec); // http, https, file
            try (InputStream in = url.openStream()) {
                Properties p = new Properties();
                p.load(in);
                return p;
            }
        } catch (MalformedURLException e) {
            // Plain filesystem path
            try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(spec))) {
                Properties p = new Properties();
                p.load(in);
                return p;
            } catch (IOException io) {
                throw new FileNotFoundException("Config URL/path not accessible: " + spec + " (" + io.getMessage() + ")");
            }
        }
	}
    
// ---------- Helpers ----------
 
    private static boolean isNonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
 
    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (isNonBlank(v)) return v;
        }
        return null;
    }
	
// Convenience main for manual testing
    public static void main(String[] args) throws Exception {
        Properties props = PropertiesLoader.load(); // strict env resolution
        String envEffective = resolveEnvStrict();
        System.out.println("Effective env = " + envEffective);
        System.out.println("Loaded " + props.size() + " properties.");
 
        String baseUrl = props.getProperty("base.url");
        if (baseUrl != null) {
            System.out.println("base.url = " + baseUrl);
        }
    }
 
}