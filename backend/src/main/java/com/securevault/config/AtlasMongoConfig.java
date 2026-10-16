package com.securevault.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/**
 * Configures dual MongoDB connections:
 * 1. Primary MongoTemplate connected to local MongoDB (localhost:27017)
 *    used by Spring Data repositories (BackupRepository) and MongoDB Compass.
 * 2. Secondary MongoTemplate connected to MongoDB Atlas cloud
 *    used for automatic cloud backup redundancy.
 */
@Configuration
public class AtlasMongoConfig {

    private static final Logger log = LoggerFactory.getLogger(AtlasMongoConfig.class);

    @Value("${MONGODB_ATLAS_URI:}")
    private String atlasUri;

    /**
     * Primary MongoTemplate for local MongoDB (localhost:27017).
     * Explicitly marked @Primary so Spring Data repositories bind here.
     */
    @Primary
    @Bean(name = "mongoTemplate")
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory, MongoConverter converter) {
        return new MongoTemplate(factory, converter);
    }

    @Bean(name = "atlasMongoTemplate")
    public MongoTemplate atlasMongoTemplate() {
        if (atlasUri == null || atlasUri.isBlank()) {
            log.info("MONGODB_ATLAS_URI not configured — Atlas cloud sync is DISABLED");
            return null;
        }

        try {
            ConnectionString connectionString = new ConnectionString(atlasUri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();
            MongoClient atlasClient = MongoClients.create(settings);

            String database = connectionString.getDatabase() != null
                    ? connectionString.getDatabase()
                    : "VaultProject";

            log.info("Atlas cloud sync ENABLED — secondary writes to MongoDB Atlas database '{}'", database);
            return new MongoTemplate(atlasClient, database);
        } catch (Exception e) {
            log.warn("Failed to initialize Atlas MongoTemplate — cloud sync DISABLED: {}", e.getMessage());
            return null;
        }
    }
}
