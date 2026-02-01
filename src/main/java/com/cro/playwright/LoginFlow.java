package com.cro.playwright;
 
import com.microsoft.playwright.Page;
 
public final class LoginFlow {
 
    private LoginFlow() {}
 
    public static void performLogin(String role, String username, String password) {
 
        Page page = BrowserManager.getPage();
 
        // Placeholder UI logic – you will implement later
        /*
        page.fill("#username", username);
        page.fill("#password", password);
        page.click("#login");
        */
        //page.waitForURL("**/dashboard");
        
        System.out.println("===============DUMMY Login flow==================");
        System.out.println("[LOGIN] START");
        System.out.println("[LOGIN] role      = " + role);
        System.out.println("[LOGIN] username  = " + username);
        System.out.println("[LOGIN] password  = " + password);
        System.out.println("[LOGIN] thread    = " + Thread.currentThread().getName());
        System.out.println("[LOGIN] END");
        System.out.println("=================================");
        
 
        System.out.println(
            "[LOGIN] Logged in as role=" + role +
            " user=" + username +
            " thread=" + Thread.currentThread().getName()
        );
    }
}
 