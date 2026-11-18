package com.mci.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Dead simple .env file reader.
 * Reads key=value pairs from a .env file.
 *
 * No external libraries needed — just basic file reading.
 */
public class EnvLoader {

    /**
     * Reads a .env file and returns the key-value pairs as a Map.
     * Lines starting with # are comments.
     * Format: KEY=VALUE (one per line)
     */
    public static Map<String, String> load(String filePath) {
        Map<String, String> env = new HashMap<>();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Warning: .env file not found at " + filePath);
            return env;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Split on first '=' only
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String key = line.substring(0, eqIndex).trim();
                    String value = line.substring(eqIndex + 1).trim();
                    env.put(key, value);
                }
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Error reading .env file: " + e.getMessage());
        }

        return env;
    }

    /**
     * Gets a value from the .env file, or falls back to system environment variable.
     */
    public static String get(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }
}
