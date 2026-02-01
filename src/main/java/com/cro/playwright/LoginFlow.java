/*
* IMP: LoginFlow → Business logic + session handling
 
This is where your role + session JSON logic belongs.
*/
package com.cro.playwright;
 
import com.cro.pages.LoginPage;
 
public class LoginFlow {
 
    private final LoginPage loginPage;
 
    public LoginFlow(LoginPage loginPage) {
        this.loginPage = loginPage;
    }
 
    public void performLogin(String role, String user, String password) {
    	System.out.println(
    	        "[FLOW] LoginFlow invoked | role=" + role +
    	        " user=" + user +
    	        " thread=" + Thread.currentThread().getName()
    	    );
        loginPage.login(user, password);
    }
}