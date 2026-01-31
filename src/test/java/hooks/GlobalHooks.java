package hooks;

import java.io.IOException;

import java.util.Properties;

import com.aventstack.extentreports.service.ExtentService;

import com.cro.playwright.BrowserManager;

import com.cro.settings.PathConfig;

import com.cro.settings.PathManager;

import com.cro.settings.PropertiesLoader;

import io.cucumber.java.AfterAll;

import io.cucumber.java.BeforeAll;

public class GlobalHooks {

	private static Properties config;

	private static String baseUrl;  //static because common for entire run

	private static String env; //static because common for entire run

	private static String OSName; //OS name for extent report

	private static String browserVersion;

	private static String applicationURL;

	@BeforeAll

    public static void suiteInit() throws IOException {

		//JVM prining JVM ID and thread count.

		printRuntimeInfo();

		// 1) Ensure output dirs exist (idempotent)

        PathManager.createRequiredDirs();

        // 2) Load env config once (cached)

        config = PropertiesLoader.loadCached();

        env = PropertiesLoader.effectiveEnv();

        OSName=System.getProperty("os.name");

        String browser = PropertiesLoader.effectiveBrowserCached(); //non static because shared in each scneario

        applicationURL=PropertiesLoader.loadCached().getProperty("base.url");        

     // 2a) Read base.url with fail-fast

        baseUrl = config.getProperty("base.url");

        if (baseUrl == null || baseUrl.isBlank()) {

            throw new IllegalStateException("Missing required property 'base.url' for env: " + env);

        }

        System.out.println("[GlobalHooks] Loaded env config for: " + env);

        System.out.println("[GlobalHooks] Effective Browser: " + browser);

        System.out.println("[GlobalHooks] Base URL: " + baseUrl);

        // 3) Optional diagnostics: where path config came from + values

        PathConfig.dump(msg -> System.out.println("[PathConfig] " + msg));

        // 4) Extent report Info (ENV + BROWSER added here)

        ExtentService.getInstance().setSystemInfo("Environment", env);

        ExtentService.getInstance().setSystemInfo("OS Version",OSName);

        ExtentService.getInstance().setSystemInfo("Browser", browser);        

        ExtentService.getInstance().setSystemInfo("Execution URL",applicationURL);

        ExtentService.getInstance().setSystemInfo("Base Dir",        PathManager.baseDirPath().toString());

        ExtentService.getInstance().setSystemInfo("Reports Dir",     PathManager.reportDir().toString());

        ExtentService.getInstance().setSystemInfo("Screenshots Dir", PathManager.screenshotDir().toString());

        ExtentService.getInstance().setSystemInfo("Logs Dir",        PathManager.logDir().toString());

        ExtentService.getInstance().setSystemInfo("Downloads Dir",   PathManager.downloadDir().toString());

        System.out.println("dp.threads=" + System.getProperty("dp.threads") + " | Thread=" + Thread.currentThread().getName());
 
 
        

    }

	//This method will return values of the provided key

	public static String getConfigValue(String key) {

        if (config == null) throw new IllegalStateException("Config not initialized. Did @BeforeAll run?");

        return config.getProperty(key);

    }

	// Convenient getter for baseUrl

    public static String getBaseUrl() {

        if (baseUrl == null || baseUrl.isBlank()) {

            throw new IllegalStateException("Base URL not initialized. Did @BeforeAll run?");

        }

        return baseUrl;

    }

    //Method to publish JVM ID and thread count on CMD, in framework fork harcoded=1, so it must return single JVM ID.

    public static void printRuntimeInfo() {

        System.out.println(

            "=== RUNTIME INFO ===\n" +

            "JVM PID        = " + ProcessHandle.current().pid() + "\n" +

            "dp.threads    = " + System.getProperty("dp.threads") + "\n" +

            "==============="

        );

    }

	@AfterAll

    public static void globalTeardown() {

        System.out.println("Run completed. Cleanup if needed.");        

        BrowserManager.closePlaywright();

    }

}
 