package com.cro.playwright;
 
public final class LoginFlow {
 
    private LoginFlow() {}
 
    public static void performLogin(String role) {
 
        var page = BrowserManager.getPage();
        /*
        var creds = CredentialProvider.forRole(role);
 
        page.fill("#username", creds.username());
        page.fill("#password", creds.password());
        page.click("#login");
        */
 
        //page.waitForURL("**/dashboard");
    
    
    }
}