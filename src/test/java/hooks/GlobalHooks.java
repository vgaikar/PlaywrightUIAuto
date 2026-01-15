package hooks;
 
import java.io.IOException;
import java.util.Properties;
 
import com.aventstack.extentreports.service.ExtentService;
import com.cro.settings.PropertiesLoader;
 
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
 
public class GlobalHooks {
	private static Properties config;
 
	@BeforeAll
	public static void loadConfig() throws IOException {
		// Load properties once before all scenarios
		config = PropertiesLoader.load();
		String env = System.getProperty("env");
		System.out.println("[GlobalHooks] Loaded config for env: " + env);
		// Push environment/system info into Extent report (run-level)
	    ExtentService.getInstance().setSystemInfo("Environment", env);
 
	}
 
	public static String getConfigValue(String key) {
		if (config == null) {
			throw new IllegalStateException("Config not initialized. Did @BeforeAll run?");
		}
		return config.getProperty(key);
	}
 
	@BeforeAll
	public static void loggingConfig() {
		System.out.println("This is to load log4j2 logging configuration file");
	}
 
	@BeforeAll
	public static void extentReportConfig() {
		System.out.println("This is to load Extent Report configuration file");
	}
 
	@AfterAll
	public static void globalTeardown() {
		System.out.println("Cleaning up resources including env, log4j2 and Extent Report.");
		System.out.println("Order will figure out during actual implementation.");
	}
 
}