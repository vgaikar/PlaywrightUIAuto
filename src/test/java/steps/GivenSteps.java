package steps;
 
import hooks.GlobalHooks;
import io.cucumber.java.en.Given;
 
public class GivenSteps {
	@Given("the user is on {string} page")
	public void userIsOnPage(String string) {
 
		String baseUrl = GlobalHooks.getConfigValue("base.url");
		System.out.println("Navigating to: " + baseUrl);
 
	}
 
}