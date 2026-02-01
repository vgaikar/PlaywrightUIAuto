/*
* This class is needed as picocontainer can't directly interact with playwright object to create pages.
* PicoContainer only knows how to create classes, not runtime objects like:
Playwright
Browser
BrowserContext
Page (ThreadLocal, created in hooks)
This class will get Threadsafe Page of Playwright and pass it to the UIActions class, so picontainer will generate
required objects at scenario level.
*/
package com.cro.playwright;
 
import com.microsoft.playwright.Page;
 
public class PageProvider {
 
    public Page get() {
        return BrowserManager.getPage();
    }
}
 