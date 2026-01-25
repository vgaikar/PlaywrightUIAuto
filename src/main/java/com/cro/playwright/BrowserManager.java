package com.cro.playwright;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.microsoft.playwright.*;

public class BrowserManager {
	private static Playwright playwright;
	 
    private static final ThreadLocal<BrowserContext> context = new ThreadLocal<>();
    private static final ThreadLocal<Page> page = new ThreadLocal<>();
 
    // Screen size for Firefox (guaranteed maximize)
    private static final Dimension screenSize =  Toolkit.getDefaultToolkit().getScreenSize();
    private static final int width = (int) screenSize.getWidth();
    private static final int height = (int) screenSize.getHeight();
 
    /* =========================================================
       Runs ONCE – @BeforeAll
       ========================================================= */
    public static synchronized void initBrowser() {
        if (playwright != null) return;
        playwright = Playwright.create();
    }
 
    /* =========================================================
       Runs PER SCENARIO – @Before
       Persistent + Clean + Maximized + Thread-safe
       ========================================================= */
    public static void createContext(String browserType) {
 
        switch (browserType.toLowerCase()) {
 
            case "chrome": {
                Path profilePath = Paths.get(
                        System.getProperty("user.dir"),
                        "Sessions",
                        "ChromeProfile_" + Thread.currentThread().getId()
                );
 
                BrowserContext ctx =
                        playwright.chromium().launchPersistentContext(
                                profilePath,
                                new BrowserType.LaunchPersistentContextOptions()
                                        .setHeadless(false)
                                        .setChannel("chrome")
                                        .setArgs(List.of("--start-maximized"))
                                        .setViewportSize(null)
                        );
 
                context.set(ctx);
                page.set(ctx.pages().get(0));
                break;
            }
 
            case "edge": {
                Path profilePath = Paths.get(
                        System.getProperty("user.dir"),
                        "Sessions",
                        "EdgeProfile_" + Thread.currentThread().getId()
                );
 
                BrowserContext ctx =
                        playwright.chromium().launchPersistentContext(
                                profilePath,
                                new BrowserType.LaunchPersistentContextOptions()
                                        .setHeadless(false)
                                        .setChannel("msedge")
                                        .setArgs(List.of("--start-maximized"))
                                        .setViewportSize(null)
                        );
 
                context.set(ctx);
                page.set(ctx.pages().get(0));
                break;
            }
 
            case "firefox": {
                Path profilePath = Paths.get(
                        System.getProperty("user.dir"),
                        "Sessions",
                        "FirefoxProfile_" + Thread.currentThread().getId()
                );
 
                BrowserContext ctx =
                        playwright.firefox().launchPersistentContext(
                                profilePath,
                                new BrowserType.LaunchPersistentContextOptions()
                                        .setHeadless(false)
                                        .setViewportSize(width, height) // 🔥 real maximize
                        );
 
                context.set(ctx);
                page.set(ctx.pages().get(0));
                break;
            }
 
            default: //fallback -> if browser other than chrome, edge, firfox this will trigger example Safari
                throw new IllegalArgumentException(
                        "Unsupported browser: " + browserType
                );
        }
    }
 
    public static Page getPage(){    	
        return page.get();
    }
 
    /* =========================================================
       Runs PER SCENARIO – @After
       ========================================================= */
    public static void closeContext() {
    	 if (context.get() != null) {
            context.get().close();
        }
        context.remove();
        page.remove();
    }
 
    /* =========================================================
       Runs ONCE – @AfterAll
       ========================================================= */
    public static void closePlaywright() {
        if (playwright != null) {
            playwright.close();
        }
    }
 
}
