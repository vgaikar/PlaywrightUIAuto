package com.cro.playwright;
 
import com.aventstack.extentreports.service.ExtentService;
 
public final class BrowserInfo {
 
    private static volatile boolean published = false;
 
    public static void captureOnce(String version) {
        if (!published) {
            synchronized (BrowserInfo.class) {
                if (!published) {
                    ExtentService.getInstance()
                            .setSystemInfo("Browser Version", version);
                    published = true;
                }
            }
        }
    }
}