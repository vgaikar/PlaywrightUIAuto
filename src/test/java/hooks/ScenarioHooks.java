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
	@Before(order = 0)
	public void before(Scenario scenario) throws IOException {
		 // 🔐 READ CREDENTIALS FROM PROPERTIES (NEW)	  
	    String browser = PropertiesLoader.effectiveBrowserCached();
	    String role = RoleResolver.resolve(scenario);
	    final String username=PropertiesLoader.getUsernameForRole(role);
	    //Push user and role in Extent report
	    ExtentReportMetada.put("User [Role: " + role + "]", username);
	    final String password = PropertiesLoader.getPasswordForRole(role);
	    BrowserManager.initBrowser(browser);
	    BrowserInfo.captureOnce(BrowserManager.getBrowserVersion());
	    Path sessionPath = SessionManager.getOrCreateSession(role,username, () -> {
	        // 🔐 First thread only per role
	        BrowserManager.createContext();
 
	        try {
	            BrowserManager.getPage().navigate(
	                PropertiesLoader.loadCached().getProperty("base.url")
	            );	            
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
 
	        LoginFlow.performLogin(role, username, password);
	        // 🔐 Persist session
	        BrowserManager.getContext().storageState(
	            new BrowserContext.StorageStateOptions()
	                .setPath(PathManager.sessionDir().resolve(role + "_" + username+ ".json"))
	        );
	        BrowserManager.closeContext();
	    });
 
	    // 🚀 Always fresh context per scenario
	    BrowserManager.createContext(sessionPath);
	    System.out.println(
	        "Thread=" + Thread.currentThread().getName() +
	        " BrowserHash=" + System.identityHashCode(BrowserManager.getBrowserVersion())
	    );	 
	}
	@BeforeStep
	public void beforeStep() {
		ScenarioContext.markStepStart();
	}
	@AfterStep
	public void afterStep(Scenario scenario) {
		//long ms = ScenarioContext.stepDuration();
		//LogBridge.step("Step completed in " + ms + " ms");
		//ScenarioContext.clearStepTiming();
		//LogBridge.step("Step completed");
	}
	@After
	public void after(Scenario scenario) {
	    BrowserManager.closeContext();	    
	}
 
}