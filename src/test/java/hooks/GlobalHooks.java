package hooks;
 
import java.io.IOException;
import java.util.Properties;
 
import com.aventstack.extentreports.service.ExtentService;
import com.cro.settings.PathConfig;
import com.cro.settings.PathManager;
import com.cro.settings.PropertiesLoader;
 
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
 
public class GlobalHooks {
	private static Properties config;
	@BeforeAll
    public static void suiteInit() throws IOException {
        // 1) Ensure output dirs exist (idempotent)
        PathManager.createRequiredDirs();
 
        // 2) Load env config once (cached)
        config = PropertiesLoader.loadCached();
        String env = PropertiesLoader.effectiveEnv();
        String browser = PropertiesLoader.effectiveBrowserCached();
        System.out.println("[GlobalHooks] Loaded env config for: " + env);
        System.out.println("[GlobalHooks] Effective Browser: " + browser);
 
        // 3) Optional diagnostics: where path config came from + values
        PathConfig.dump(msg -> System.out.println("[PathConfig] " + msg));
 
        // 4) Extent System Info (ENV + BROWSER added here)
        ExtentService.getInstance().setSystemInfo("Environment", env);
        ExtentService.getInstance().setSystemInfo("Browser", browser);
        ExtentService.getInstance().setSystemInfo("Base Dir",        PathManager.baseDirPath().toString());
        ExtentService.getInstance().setSystemInfo("Reports Dir",     PathManager.reportDir().toString());
        ExtentService.getInstance().setSystemInfo("Screenshots Dir", PathManager.screenshotDir().toString());
        ExtentService.getInstance().setSystemInfo("Logs Dir",        PathManager.logDir().toString());
        ExtentService.getInstance().setSystemInfo("Downloads Dir",   PathManager.downloadDir().toString());
    }
	public static String getConfigValue(String key) {
        if (config == null) throw new IllegalStateException("Config not initialized. Did @BeforeAll run?");
        return config.getProperty(key);
    }
	@AfterAll
    public static void globalTeardown() {
        System.out.println("Run completed. Cleanup if needed.");
    }
}