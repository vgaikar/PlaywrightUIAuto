package com.cro.playwright;

/*
* This is the glue PicoContainer gives you.
* One instance of playwright page and session file per scenario
Shared everywhere automatically
*/

import java.nio.file.Path;

import com.microsoft.playwright.Page;

public class TestContext {
	private Page page;
	private Path sessionFile;

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public Path getSessionFile() {
		return sessionFile;
	}

	public void setSessionFile(Path sessionFile) {
		this.sessionFile = sessionFile;
	}

}