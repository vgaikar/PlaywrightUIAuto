package com.cro.pages;
 
import com.cro.base.BasePage;
import com.cro.utils.UIActions;
 
public class LoginPage extends BasePage {
 
    private static final String USERNAME = "#userNameInput";
    private static final String PASSWORD = "#passwordInput";
    private static final String LOGIN_BTN = "#loginBtn";
 
    public LoginPage(UIActions uiActions) {
        super(uiActions);
    }
 
    public void login(String user, String pass) {
    	System.out.println(
    	        "[PAGE] LoginPage.login() | pageHash=" +
    	        System.identityHashCode(uiActions) +
    	        " thread=" + Thread.currentThread().getName()
    	    );
    	/*
    	uiActions.fill(USERNAME, user);
    	uiActions.fill(PASSWORD, pass);
    	uiActions.click(LOGIN_BTN);
    	*/
    }
}
 