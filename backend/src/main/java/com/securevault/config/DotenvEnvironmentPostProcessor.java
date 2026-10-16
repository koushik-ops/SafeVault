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
                ".env",
                "backend/.env",
                "../.env",
                "../backend/.env"
        };

        for (String path : potentialPaths) {
            File envFile = new File(path);
            if (envFile.exists() && envFile.isFile()) {
                loadEnvFile(envFile, environment);
                break;
            }
        }
    }

    private void loadEnvFile(File file, ConfigurableEnvironment environment) {
        Map<String, Object> envProperties = new HashMap<>();
        try (InputStream is = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(is);

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value != null && !value.trim().isEmpty()) {
                    String trimmedKey = key.trim();
                    String trimmedValue = value.trim();

                    // If DATABASE_URL is still placeholder, ignore it so dev profile uses in-memory H2
                    if (trimmedKey.equals("DATABASE_URL") && trimmedValue.contains("YOUR_SUPABASE_HOST")) {
                        continue;
                    }

                    envProperties.put(trimmedKey, trimmedValue);
                    if (System.getProperty(trimmedKey) == null) {
                        System.setProperty(trimmedKey, trimmedValue);
                    }
                }
            }

            if (!envProperties.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", envProperties));
            }
        } catch (Exception ignored) {
            // Silently continue if .env parsing encounters non-fatal issue
        }
    }
}
