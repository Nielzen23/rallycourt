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
import java.util.Arrays;

@Configuration
public class MongoLiquibaseConfig {

    @Bean
    @ConditionalOnProperty(name = "app.mongo.liquibase.enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner mongoLiquibaseRunner(
            @Value("${spring.data.mongodb.uri:}") String mongoUri,
            @Value("${app.mongo.liquibase.contexts:}") String mongoLiquibaseContexts) {
        return args -> {
            if (!StringUtils.hasText(mongoUri)) {
                return;
            }
            runMongoLiquibase(mongoUri, mongoLiquibaseContexts);
        };
    }

    private void runMongoLiquibase(String mongoUri, String mongoLiquibaseContexts) throws LiquibaseException {
        Database database = DatabaseFactory.getInstance()
                .openDatabase(mongoUri, null, null, null, new ClassLoaderResourceAccessor());

        try (database) {
            Liquibase liquibase = new Liquibase(
                    "db/changelog/mongodb/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(resolveContexts(mongoLiquibaseContexts), new LabelExpression());
        }
    }

    private Contexts resolveContexts(String mongoLiquibaseContexts) {
        if (!StringUtils.hasText(mongoLiquibaseContexts)) {
            return new Contexts();
        }

        String[] contexts = Arrays.stream(mongoLiquibaseContexts.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);

        return contexts.length == 0 ? new Contexts() : new Contexts(contexts);
    }
}
