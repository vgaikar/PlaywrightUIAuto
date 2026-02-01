package hooks;
 
import java.io.IOException;
import java.nio.file.Path;
 
import com.cro.extentreporting.ExtentReportMetada;
import com.cro.listeners.ScenarioContext;
import com.cro.playwright.BrowserInfo;
import com.cro.playwright.BrowserManager;
import com.cro.playwright.LoginFlow;
import com.cro.playwright.RoleResolver;
import com.cro.playwright.SessionManager;
import com.cro.settings.PathManager;
import com.cro.settings.PropertiesLoader;
import com.microsoft.playwright.BrowserContext;
 
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeStep;
import io.cucumber.java.Scenario;
 
public class ScenarioHooks {
 
    private final LoginFlow loginFlow;
 
    // 🔥 PicoContainer injects LoginFlow (and its dependencies)
    public ScenarioHooks(LoginFlow loginFlow) {
        this.loginFlow = loginFlow;
    }
 
    @Before(order = 0)
    public void before(Scenario scenario) throws IOException {
 
        // =========================
        // Resolve execution inputs
        // =========================
        String browser = PropertiesLoader.effectiveBrowserCached();
        String role = RoleResolver.resolve(scenario);
        String username = PropertiesLoader.getUsernameForRole(role);
        String password = PropertiesLoader.getPasswordForRole(role);
 
        // Push metadata to Extent
        ExtentReportMetada.put("User [Role: " + role + "]", username);
 
        // =========================
        // Browser init (ThreadLocal)
        // =========================
        BrowserManager.initBrowser(browser);
        BrowserInfo.captureOnce(BrowserManager.getBrowserVersion());
 
        // =========================
        // Session handling (role+user)
        // =========================
        Path sessionPath = SessionManager.getOrCreateSession(role, username, () -> {
 
            // 🔐 First thread per role+user only
            BrowserManager.createContext();
 
            try {
                BrowserManager.getPage().navigate(
                        PropertiesLoader.loadCached().getProperty("base.url")
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }           
            // ✅ NON-STATIC flow call (Pico managed)
            loginFlow.performLogin(role, username, password);
 
            // Persist storage state
            BrowserManager.getContext().storageState(
                new BrowserContext.StorageStateOptions()
                    .setPath(PathManager.sessionDir()
                        .resolve(role + "_" + username + ".json"))
            );
 
            BrowserManager.closeContext();
        });
 
        // =========================
        // Fresh context per scenario
        // =========================
        BrowserManager.createContext(sessionPath);
 
        System.out.println(
            "[HOOK] Thread=" + Thread.currentThread().getName() +
            " Browser=" + BrowserManager.getBrowserVersion()
        );
        System.out.println(
        	    "[DEBUG] Thread=" + Thread.currentThread().getName()
        	);
 
        	System.out.println(
        	    "[DEBUG] BrowserVersion=" + BrowserManager.getBrowserVersion()
        	);
 
        	System.out.println(
        	    "[DEBUG] ContextHash=" +
        	    System.identityHashCode(BrowserManager.getContext())
        	);
 
        	System.out.println(
        	    "[DEBUG] PageHash=" +
        	    System.identityHashCode(BrowserManager.getPage())
        	);
 
    }
 
    @BeforeStep
    public void beforeStep() {
        ScenarioContext.markStepStart();
    }
 
    @AfterStep
    public void afterStep(Scenario scenario) {
        // future: step-level logging / screenshots
    }
 
    @After
    public void after(Scenario scenario) {
        BrowserManager.closeContext();
    }
}