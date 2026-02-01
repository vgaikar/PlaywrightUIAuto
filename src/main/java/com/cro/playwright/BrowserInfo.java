/**
* Publishes browser version information to Extent Reports exactly once per test run.
*
* Designed for parallel execution: the first scenario that initializes the browser
* records the version, and all subsequent scenarios skip publishing to avoid
* duplicate or conflicting system info entries.
*/
 
package com.cro.playwright;
 
import com.cro.extentreporting.ExtentReportMetada;
 
public final class BrowserInfo {
 
    private static volatile boolean published = false;
 
    public static void captureOnce(String version) {
        if (!published) {
            synchronized (BrowserInfo.class) {
                if (!published) {
                	ExtentReportMetada.put("Browser Version", version);                    
                    published = true;
                }
            }
        }
    }
}
 