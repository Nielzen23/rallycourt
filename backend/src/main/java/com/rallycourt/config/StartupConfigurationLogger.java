package com.rallycourt.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@Configuration
public class StartupConfigurationLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupConfigurationLogger.class);

    @Bean
    public ApplicationRunner startupDiagnosticsRunner(
            Environment environment,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.data.mongodb.uri:}") String mongodbUri,
            @Value("${app.jwt.secret:}") String jwtSecret,
            @Value("${app.geocoding.geoapify.api-key:}") String geoapifyApiKey
    ) {
        return args -> {
            String[] activeProfiles = environment.getActiveProfiles();
            LOGGER.info(
                    "Startup profiles: {}",
                    activeProfiles.length == 0 ? "[default]" : Arrays.toString(activeProfiles)
            );
            LOGGER.info("PostgreSQL configured: {}", summarizeJdbcUrl(datasourceUrl));
            LOGGER.info("MongoDB configured: {}", summarizeMongoUri(mongodbUri));
            LOGGER.info("JWT secret configured: {}", describeSecret(jwtSecret));
            LOGGER.info("Geoapify API key configured: {}", describeSecret(geoapifyApiKey));
        };
    }

    private String summarizeJdbcUrl(String datasourceUrl) {
        if (!StringUtils.hasText(datasourceUrl)) {
            return "missing";
        }
        return datasourceUrl;
    }

    private String summarizeMongoUri(String mongodbUri) {
        if (!StringUtils.hasText(mongodbUri)) {
            return "missing";
        }

        try {
            URI uri = new URI(mongodbUri);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            return "host=" + (host == null ? "unknown" : host)
                    + ", port=" + (port < 0 ? "default" : port)
                    + ", database=" + (StringUtils.hasText(path) ? path.replaceFirst("^/", "") : "unknown");
        } catch (URISyntaxException exception) {
            return "present (unparsed)";
        }
    }

    private String describeSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return "missing";
        }
        return "present (length=" + value.length() + ")";
    }
}
