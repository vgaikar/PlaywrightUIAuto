package com.cro.playwright;
 
import io.cucumber.java.Scenario;
 
public final class RoleResolver {
 
    private static final String ROLE_PREFIX = "@role_";  //Prefix needed to resolve cucumber tag
 
    private RoleResolver() {}
 
    public static String resolve(Scenario scenario) {
 
        // 1️⃣ CLI override, always prefix with @role_ from CLI
        String cliRole = System.getProperty("role");
        if (cliRole != null && !cliRole.isBlank()) {
            return cliRole.trim().toLowerCase();
        }
 
        // 2️⃣ Scenario tag
        return scenario.getSourceTagNames().stream()
                .filter(t -> t.startsWith(ROLE_PREFIX))
                .map(t -> t.replace(ROLE_PREFIX, ""))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No role found. Provide -Drole= or use @role_* tag"));
    }
}