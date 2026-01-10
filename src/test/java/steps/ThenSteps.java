package steps;

import io.cucumber.java.en.Then;

public class ThenSteps {
	@Then("the user should be on the {string} tab")
	public void the_user_should_be_on_the_tab(String string) {
	   
	}
	@Then("the {string} tab displays following contols with default values")
	public void the_tab_displays_following_contols_with_default_values(String string, io.cucumber.datatable.DataTable dataTable) {
	    
	}
	@Then("the country {string} appears in the list")
	public void the_country_appears_in_the_list(String string) {
	  
	}
	
	//company logo check
	@Then("the company logo {string} is visible on the top navigation bar")
	public void the_company_logo_is_visible_on_the_top_navigation_bar(String string) {
	}

}
