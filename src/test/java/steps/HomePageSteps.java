package steps;
 
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
 
public class HomePageSteps {
	@When("the user navigates to the homepage using the company logo")
	public void the_user_navigates_to_the_homepage_using_the_company_logo() {	
		System.out.println("the user navigates to the homepage using the company logo");
	}	
	@Then("user is presented by welcome {string} text")
	public void user_is_presented_by_welcome_text(String string) {
		System.out.println("User presented by welcome text- 'Welcome to CTMS Portal'");
	}
}