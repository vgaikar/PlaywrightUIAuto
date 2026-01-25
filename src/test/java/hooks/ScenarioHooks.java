
package hooks;

import io.cucumber.java.*;

import com.cro.listeners.ScenarioContext;
import com.cro.playwright.BrowserManager;
import com.cro.settings.PropertiesLoader;

import java.io.IOException;

import org.apache.logging.log4j.ThreadContext;

import com.cro.listeners.LogBridge;

public class ScenarioHooks {

	@Before
	public void before(Scenario scenario) throws IOException {
		ScenarioContext.init(scenario.getName().replaceAll("[^a-zA-Z0-9-_]", "_"));
		LogBridge.info("[Before] " + scenario.getName());
		ThreadContext.put("threadId", String.valueOf(Thread.currentThread().threadId()));
		ThreadContext.put("uuid", java.util.UUID.randomUUID().toString());
		//reading browser name from JVM or Environment variable or properties file.
		String browser = PropertiesLoader.effectiveBrowserCached();
		BrowserManager.createContext(browser);

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
		if (scenario.isFailed()) {
			LogBridge.error("Scenario failed");
		}
		ScenarioContext.clear();
		BrowserManager.closeContext();
	}
}
