package com.rallycourt.config;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class MongoLiquibaseConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mongo.liquibase.enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner mongoLiquibaseRunner(
            @Value("${spring.data.mongodb.uri:}") String mongoUri) {
        return args -> {
            if (!StringUtils.hasText(mongoUri)) {
                return;
            }
            runMongoLiquibase(mongoUri);
        };
    }

    private void runMongoLiquibase(String mongoUri) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, new ClassLoaderResourceAccessor());

        try (database) {
            Liquibase liquibase = new Liquibase(
                    "db/changelog/mongodb/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }
}
