package com.securevault.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Automatically loads .env file into Spring Boot Environment before context starts.
 * Searches in current directory, backend directory, and parent directory.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] potentialPaths = {
                "atlas-credentials.env",
                "../atlas-credentials.env",
                "backend/atlas-credentials.env",
                ".env",
                "backend/.env",
                "../.env",
                "../backend/.env"
        };

        Map<String, Object> allProperties = new HashMap<>();
        for (String path : potentialPaths) {
            File envFile = new File(path);
            if (envFile.exists() && envFile.isFile()) {
                loadEnvFile(envFile, allProperties);
            }
        }

        if (!allProperties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", allProperties));
        }
    }

    private void loadEnvFile(File file, Map<String, Object> envProperties) {
        boolean isAtlasFile = file.getName().toLowerCase().contains("atlas");
        try (InputStream is = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(is);

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value != null && !value.trim().isEmpty()) {
                    String trimmedKey = key.trim();
                    String trimmedValue = value.trim();

                    // Strip wrapping double or single quotes
                    if ((trimmedValue.startsWith("\"") && trimmedValue.endsWith("\"")) ||
                        (trimmedValue.startsWith("'") && trimmedValue.endsWith("'"))) {
                        if (trimmedValue.length() >= 2) {
                            trimmedValue = trimmedValue.substring(1, trimmedValue.length() - 1);
                        }
                    }

                    // If loading from atlas-credentials.env, route MONGODB_URI to MONGODB_ATLAS_URI
                    // so local MONGODB_URI (localhost:27017) is not overwritten
                    if (isAtlasFile && trimmedKey.equals("MONGODB_URI")) {
                        trimmedKey = "MONGODB_ATLAS_URI";
                        if (!trimmedValue.contains("/VaultProject") && !trimmedValue.contains("/securevault")) {
                            trimmedValue = trimmedValue.replaceAll("/+$", "") + "/VaultProject?retryWrites=true&w=majority";
                        }
                    }

                    envProperties.put(trimmedKey, trimmedValue);
                    System.setProperty(trimmedKey, trimmedValue);
                }
            }
        } catch (Exception ignored) {
            // Silently continue if .env parsing encounters non-fatal issue
        }
    }
}
