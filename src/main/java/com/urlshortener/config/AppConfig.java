package com.urlshortener.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private final Properties properties = new Properties();

    public AppConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public int getDefaultTtlMinutes() {
        return Integer.parseInt(properties.getProperty("app.default.ttl.minutes", "1440")); // 24 hours default
    }

    public int getDefaultLimit() {
        return Integer.parseInt(properties.getProperty("app.default.limit", "5"));
    }

    public int getCleanupIntervalSeconds() {
        return Integer.parseInt(properties.getProperty("app.cleanup.interval.seconds", "60"));
    }
}
