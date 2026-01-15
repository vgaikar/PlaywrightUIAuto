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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class PropertiesLoader {

	// Flat filenames (no env subfolders)
	private static final String ENV_FILE_TEMPLATE = "config/config-%s.properties";

	// Default env discovery files (classpath)
	private static final String TEST_DEFAULT_ENV_FILE = "config/config-test-default.properties";
	private static final String MAIN_DEFAULT_ENV_FILE = "config/config-app-default.properties";

	private PropertiesLoader() {
		// this will avoid constructor overloading by external program
	}

	/**
	 * Entry point: resolve env strictly (no dev fallback), then load properties.
	 */
	public static Properties load() throws IOException {
		String env = normalizeEnv(resolveEnvStrict());
		return loadForEnv(env);
	}

	/**
	 * Load properties for a given env (assumes env already resolved).
	 */
	public static Properties loadForEnv(String env) throws IOException {
		String normalizedEnv = normalizeEnv(env);
		if (!isNonBlank(normalizedEnv)) {
			throw new IllegalArgumentException("env is required (dev/val/uat etc.).");
		}

		// 1) Allow direct URL/file override — highest priority
		String urlSpec = firstNonBlank(System.getProperty("configUrl"), System.getenv("CONFIG_URL"));
		if (isNonBlank(urlSpec)) {
			return loadFromUrlOrFileSpec(urlSpec);
		}

		// 2) Classpath (TEST precedes MAIN automatically during test runs)
		String resourcePath = String.format(ENV_FILE_TEMPLATE, normalizedEnv);

		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = PropertiesLoader.class.getClassLoader();

		try (InputStream inputstream = cl.getResourceAsStream(resourcePath)) {
			if (inputstream != null) {
				Properties prop = new Properties();
				try (Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8)) {
					prop.load(reader); // correctly decodes UTF-8
				}

				return prop;
			}
		}

		// Not found — list attempted location(s)
		throw new FileNotFoundException("Config not found for env=" + normalizedEnv + ". Tried classpath: "
				+ resourcePath + (isNonBlank(urlSpec) ? " and URL=" + urlSpec : ""));

	}

	// ---------- Env resolution (STRICT: NO default 'dev') ----------
	/**
	 * Resolve env strictly: 1) -Denv 2) ENV env var 3) classpath test default file
	 * 4) classpath main/app default file 5) else throw
	 *
	 * @throws IOException
	 */
	private static String resolveEnvStrict() throws IOException {
		// 1) JVM property
		String envProp = System.getProperty("env");
		if (isNonBlank(envProp))
			return envProp.trim();

		// 2) ENV variable
		String envVar = System.getenv("ENV");
		if (isNonBlank(envVar))
			return envVar.trim();

		// 3) Default from test file
		String envFromTestDefault = readEnvFromClasspath(TEST_DEFAULT_ENV_FILE);
		if (isNonBlank(envFromTestDefault))
			return envFromTestDefault.trim();

		// 4) Default from main/app file
		String envFromMainDefault = readEnvFromClasspath(MAIN_DEFAULT_ENV_FILE);
		if (isNonBlank(envFromMainDefault))
			return envFromMainDefault.trim();

		// 5) Fail (no dev fallback)
		throw new IllegalArgumentException("Environment not resolved. Expected one of:\n"
				+ "  - JVM property: -Denv=<dev|val|uat>\n" + "  - Environment variable: ENV=<dev|val|uat>\n"
				+ "  - Classpath default (test): " + TEST_DEFAULT_ENV_FILE + " with key 'env' or 'ENV'\n"
				+ "  - Classpath default (main): " + MAIN_DEFAULT_ENV_FILE + " with key 'env' or 'ENV'\n");

	}

	/**
	 * Read env value from a classpath properties file using keys 'env' or 'ENV'.
	 *
	 * @param resourcePath classpath resource path
	 * @return env value if found and non-blank, else null
	 */

	private static String readEnvFromClasspath(String resourcePath) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = PropertiesLoader.class.getClassLoader();

		try (InputStream inputstream = cl.getResourceAsStream(resourcePath)) {
			if (inputstream == null)
				return null;
			Properties defaults = new Properties();
			try (Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8)) {
				defaults.load(reader); // correctly decodes UTF-8
			}

			// Case-insensitive search for "env"
			for (String propName : defaults.stringPropertyNames()) {
				if ("env".equalsIgnoreCase(propName)) {
					String val = defaults.getProperty(propName);
					if (isNonBlank(val)) {
						return val.trim(); // normalize value
					}
				}
			}

			return null;
		}
	}

	// ---------- URL (and file spec) loader ----------
	/**
	 * Load properties from a URL spec. Supports http(s):// and file:///. If a plain
	 * filesystem path is provided, attempts to read it directly as a file.
	 */
	private static Properties loadFromUrlOrFileSpec(String spec) throws IOException {
		try {
			URL url = URI.create(spec).toURL(); // http, https, file
			try (InputStream inputstream = url.openStream()) {
				Properties prop = new Properties();
				try (Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8)) {
					prop.load(reader); // correctly decodes UTF-8
				}
				return prop;
			}
		} catch (IllegalArgumentException e) {
			// Plain filesystem path
			try (InputStream inputstream = java.nio.file.Files.newInputStream(java.nio.file.Paths.get(spec))) {
				Properties prop = new Properties();
				try (Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8)) {
					prop.load(reader); // correctly decodes UTF-8
				}
				return prop;
			} catch (IOException io) {
				throw new FileNotFoundException(
						"Config URL/path not accessible: " + spec + " (" + io.getMessage() + ")");
			}
		}
	}

	// ---------- Helpers ----------

	private static boolean isNonBlank(String s) {
		return s != null && !s.trim().isEmpty();
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (isNonBlank(value))
				return value;
		}
		return null;
	}

	private static String normalizeEnv(String env) {
		return env == null ? null : env.trim().toLowerCase();
	}

}