package hooks;
import java.io.IOException;
import java.nio.file.Path;
 
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
 
	    String browser = PropertiesLoader.effectiveBrowserCached();
	    BrowserManager.initBrowser(browser);
	    BrowserInfo.captureOnce(BrowserManager.getBrowserVersion());  //this is to get browser version for extent report this is one time activity
	    String role=RoleResolver.resolve(scenario);    
 
	    Path sessionPath = SessionManager.getOrCreateSession(role, () -> {
	        // 🔐 Login flow ONLY for first thread
	        BrowserManager.createContext();
	        try {
				BrowserManager.getPage().navigate(PropertiesLoader.loadCached().getProperty("base.url"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
 
	        LoginFlow.performLogin(role);
 
	        // 🔐 Persist session
	        BrowserManager.getContext().storageState(
	                new BrowserContext.StorageStateOptions()
	                        .setPath(PathManager.sessionDir().resolve(role + ".json"))
	        );
 
	        BrowserManager.closeContext(); // cleanup temp login context
	    });
 
	    // 🚀 Always start scenario with fresh context
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