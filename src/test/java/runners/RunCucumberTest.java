package runners;

import org.testng.annotations.DataProvider;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(features = "src/test/resources/features", // feature folder path
		glue = { "steps", "hooks" },
		// tags="@smoke or @regression",// step definition path
		plugin = { "pretty", "html:target/cucumber-report.html",
				"com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:" })
public class RunCucumberTest extends AbstractTestNGCucumberTests {
	@Override
	@DataProvider(parallel = true) // <— enables scenario-level parallelism
	public Object[][] scenarios() {
		return super.scenarios();
	}
}