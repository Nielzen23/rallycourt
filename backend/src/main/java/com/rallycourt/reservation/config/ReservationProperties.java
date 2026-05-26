package com.rallycourt.reservation.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reservation")
public class ReservationProperties {

    private List<Integer> allowedDurationsMinutes = new ArrayList<>(List.of(60, 90, 120));

    public List<Integer> getAllowedDurationsMinutes() {
        return allowedDurationsMinutes;
    }

    public void setAllowedDurationsMinutes(List<Integer> allowedDurationsMinutes) {
        this.allowedDurationsMinutes = allowedDurationsMinutes;
    }
}
