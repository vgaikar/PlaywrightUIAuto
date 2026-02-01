package com.cro.utils;

/*
* Picocontainer will inject required dependency at class level of Pageprovider.
* This is utility class which will perform all the relevant UI actions on the application pages.
*/

import com.cro.playwright.PageProvider;
import com.microsoft.playwright.Page;

public class UIActions {

	private final PageProvider pageProvider;

	public UIActions(PageProvider pageProvider) {
		this.pageProvider = pageProvider;
	}

	// Page resolved ONLY when needed (after @Before)
	private Page page() {
		return pageProvider.get();
	}

	public void click(String selector) {
		page().click(selector);
	}

	public void fill(String selector, String value) {
		page().fill(selector, value);
	}

	public void navigate(String url) {
		page().navigate(url);
	}
}