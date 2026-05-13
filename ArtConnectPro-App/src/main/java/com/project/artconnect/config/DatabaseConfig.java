package com.project.artconnect.config;

/**
 * Database configuration constants.
 */
public class DatabaseConfig {
    public static final String URL = getOrDefault(
            "ARTCONNECT_DB_URL",
            "jdbc:mysql://localhost:3306/artconnect_pro?serverTimezone=Europe/Paris&useSSL=false&allowPublicKeyRetrieval=true");
    public static final String USER = getOrDefault("ARTCONNECT_DB_USER", "root");
    public static final String PASSWORD = getOrDefault("ARTCONNECT_DB_PASSWORD", "TonNouveauMdp123!");

    private DatabaseConfig() {
    }

    private static String getOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
