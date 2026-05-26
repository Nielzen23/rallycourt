package com.rallycourt.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;

class StartupConfigurationLoggerTest {

    private final StartupConfigurationLogger logger = new StartupConfigurationLogger();

    @Test
    void startupDiagnosticsRunnerExecutesWithConfiguredValues() throws Exception {
        Environment environment = new org.springframework.mock.env.MockEnvironment().withProperty("spring.profiles.active", "test");

        ApplicationRunner runner = logger.startupDiagnosticsRunner(
                environment,
                "jdbc:postgresql://localhost:5432/rallycourt",
                "mongodb://localhost:27017/rallycourt",
                "secret-value",
                "geo-key"
        );

        assertNotNull(runner);
        runner.run(null);
    }

    @Test
    void startupDiagnosticsRunnerExecutesWithMissingValues() throws Exception {
        Environment environment = new org.springframework.mock.env.MockEnvironment();

        ApplicationRunner runner = logger.startupDiagnosticsRunner(environment, "", "", "", "");

        assertNotNull(runner);
        runner.run(null);
    }

    @Test
    void summarizeMongoUriCoversValidInvalidAndMissingCases() throws Exception {
        assertEquals("missing", invokeStringMethod("summarizeMongoUri", ""));
        assertEquals(
                "host=localhost, port=27017, database=rallycourt",
                invokeStringMethod("summarizeMongoUri", "mongodb://localhost:27017/rallycourt")
        );
        assertEquals("present (unparsed)", invokeStringMethod("summarizeMongoUri", "mongodb://bad uri"));
    }

    @Test
    void summarizeJdbcUrlAndDescribeSecretCoverBothBranches() throws Exception {
        assertEquals("missing", invokeStringMethod("summarizeJdbcUrl", ""));
        assertEquals("jdbc:h2:mem:testdb", invokeStringMethod("summarizeJdbcUrl", "jdbc:h2:mem:testdb"));
        assertEquals("missing", invokeStringMethod("describeSecret", ""));
        assertEquals("present (length=6)", invokeStringMethod("describeSecret", "secret"));
    }

    private String invokeStringMethod(String methodName, String input) throws Exception {
        Method method = StartupConfigurationLogger.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (String) method.invoke(logger, input);
    }
}
