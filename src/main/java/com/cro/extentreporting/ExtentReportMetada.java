/**

* Centralized, thread-safe collector for Extent Report system metadata.

*

* Allows metadata to be gathered from multiple lifecycle hooks or threads

* and publishes it to Extent Reports exactly once per test run.

*

* Designed to avoid duplicate entries and lifecycle timing issues

* in parallel Cucumber/TestNG executions.

*/

package com.cro.extentreporting;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import com.aventstack.extentreports.service.ExtentService;

public class ExtentReportMetada {

	private static final Map<String, String> SYSTEM_INFO = new ConcurrentHashMap<>();

	private static volatile boolean published = false;

	private ExtentReportMetada() {
	}

	/** Collect metadata safely from anywhere */

	public static void put(String key, String value) {

		if (value == null || value.isBlank())
			return;

		SYSTEM_INFO.putIfAbsent(key, value);

	}

	/** Publish to Extent exactly once and in alphabetical order for readability */

	public static void publishOnce() {

		if (!published) {

			synchronized (ExtentReportMetada.class) {

				if (!published) {

					SYSTEM_INFO.entrySet().stream()

							.sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))

							.forEach(e ->

							ExtentService.getInstance()

									.setSystemInfo(e.getKey(), e.getValue())

							);

					published = true;

				}

			}

		}

	}

}
