package runners;
 
 
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
 
@CucumberOptions(
    features = "src/test/resources/features",  // feature folder path
    glue = {"steps"},
    //tags="@smoke or @regression",// step definition path
    plugin = {"pretty","html:target/cucumber-report.html"}
)
public class RunCucumberTest extends AbstractTestNGCucumberTests {}