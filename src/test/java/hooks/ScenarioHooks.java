
package hooks;

import io.cucumber.java.*;

import com.cro.listeners.ScenarioContext;
import com.cro.playwright.BrowserManager;
import com.cro.settings.PropertiesLoader;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import com.cro.listeners.LogBridge;

public class ScenarioHooks {

	@Before(order=0)
	public void before(Scenario scenario) throws IOException {

		String browser = PropertiesLoader.effectiveBrowserCached();
		BrowserManager.initBrowser(browser);
		BrowserManager.createContext();

	}

	@BeforeStep
	public void beforeStep() {
		ScenarioContext.markStepStart();
	}

	@AfterStep
	public void afterStep(Scenario scenario) {
		// long ms = ScenarioContext.stepDuration();
		// LogBridge.step("Step completed in " + ms + " ms");
		// ScenarioContext.clearStepTiming();
		// LogBridge.step("Step completed");
	}

	@After
	public void after(Scenario scenario) {
		BrowserManager.closeContext();
		BrowserManager.closePlaywright();
	}

}
