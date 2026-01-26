package com.cro.playwright;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;

import com.microsoft.playwright.*;

public final class BrowserManager {

	// 🔐 One Playwright + one Browser per thread
	private static final ThreadLocal<Playwright> TL_PLAYWRIGHT = new ThreadLocal<>();
	private static final ThreadLocal<Browser> TL_BROWSER = new ThreadLocal<>();

	// 🔁 Per-scenario objects
	private static final ThreadLocal<BrowserContext> TL_CONTEXT = new ThreadLocal<>();
	private static final ThreadLocal<Page> TL_PAGE = new ThreadLocal<>();

	// Screen size for Firefox (guaranteed maximize)
	private static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private static final int width = (int) screenSize.getWidth();
	private static final int height = (int) screenSize.getHeight();

	private BrowserManager() {
		// prevent instantiation
	}

	// =========================
	// Playwright + Browser init
	// =========================
	public static synchronized void initBrowser(String browserType) {

		if (TL_PLAYWRIGHT.get() != null) {
			return; // already initialized for this thread
		}

		Playwright playwright = Playwright.create();
		TL_PLAYWRIGHT.set(playwright);

		Browser browser;

		switch (browserType.toLowerCase()) {

		case "chrome":
			browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false)
					.setChannel("chrome").setArgs(List.of("--start-maximized")));
			break;

		case "edge":
			browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false)
					.setChannel("msedge").setArgs(List.of("--start-maximized")));
			break;

		case "firefox":
			browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false));
			break;

		default:
			throw new IllegalArgumentException("Unsupported browser: " + browserType);
		}

		TL_BROWSER.set(browser);
	}

	// =========================
	// Context + Page lifecycle
	// =========================
	public static void createContext() {

		Browser browser = TL_BROWSER.get();
		if (browser == null) {
			throw new IllegalStateException("Browser not initialized. Call initBrowser() first.");
		}

		BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(null) // real
																											// maximized
																											// window
		);

		TL_CONTEXT.set(context);
		TL_PAGE.set(context.newPage());
	}

	public static Page getPage() {
		Page page = TL_PAGE.get();
		if (page == null) {
			throw new IllegalStateException("Page not initialized. Did you call createContext()?");
		}
		return page;
	}

	// =========================
	// Cleanup
	// =========================
	public static void closeContext() {

		BrowserContext context = TL_CONTEXT.get();
		if (context != null) {
			try {
				context.close();
			} catch (Exception e) {
				System.err.println("[BrowserManager] Context close failed: " + e.getMessage());
			}
		}

		TL_CONTEXT.remove();
		TL_PAGE.remove();
	}

	public static void closePlaywright() {

		Browser browser = TL_BROWSER.get();
		if (browser != null) {
			try {
				browser.close();
			} catch (Exception e) {
				System.err.println("[BrowserManager] Browser close failed: " + e.getMessage());
			}
		}

		Playwright playwright = TL_PLAYWRIGHT.get();
		if (playwright != null) {
			try {
				playwright.close();
			} catch (Exception e) {
				System.err.println("[BrowserManager] Playwright close failed: " + e.getMessage());
			}
		}

		TL_BROWSER.remove();
		TL_PLAYWRIGHT.remove();
	}
}
