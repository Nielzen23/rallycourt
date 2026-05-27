package com.rallycourt.config;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationTimeZoneConfig {

    private final String applicationTimeZone;

    public ApplicationTimeZoneConfig(@Value("${app.timezone:Asia/Manila}") String applicationTimeZone) {
        this.applicationTimeZone = applicationTimeZone;
    }

    @PostConstruct
    void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(applicationTimeZone));
        System.setProperty("user.timezone", applicationTimeZone);
    }
}
