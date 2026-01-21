package hooks;
 
import com.cro.listeners.LogBridge;
import com.cro.listeners.ScenarioContext;
 
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
 
public class UIHooks {
	@Before(order=0)
	public void playwrightSetup() {
		System.out.println("This is to initialize playwright, context and page for browser interaction.");
	}
	@After
	public void tearDown(Scenario scenario) {
		System.out.println("*** Screenshot condition need to update for all steps performed. ***********");	
		if (scenario.isFailed()) {
 
	        // Placeholder: Screenshot
	        try {
	            LogBridge.error("Attaching screenshot (placeholder)");
	            // byte[] screenshot = PlaywrightManager.page().screenshot(...);
	            // scenario.attach(screenshot, "image/png", "Failed Screenshot");
	        } catch (Exception e) {
	            LogBridge.error("Failed to capture screenshot: " + e.getMessage());
	        }
 
	        // Placeholder: Trace
	        try {
	            LogBridge.error("Attaching trace (placeholder)");
	            // scenario.attach(Files.readAllBytes(traceFile), 
	            //                 "application/zip", "trace.zip");
	        } catch (Exception e) {
	            LogBridge.error("Failed to attach trace: " + e.getMessage());
	        }
	    }
 
	    ScenarioContext.clear();
	}
 
}