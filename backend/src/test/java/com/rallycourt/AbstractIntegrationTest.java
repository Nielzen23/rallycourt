package com.rallycourt;

import com.rallycourt.activity.service.ActivityLogService;
import com.rallycourt.court.geocoding.Coordinates;
import com.rallycourt.court.geocoding.GeocodingService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.IntegrationTestConfig.class)
public abstract class AbstractIntegrationTest {

    @TestConfiguration
    static class IntegrationTestConfig {

        @Bean
        @Primary
        GeocodingService geocodingService() {
            return new StubGeocodingService();
        }

        @Bean
        @Primary
        ActivityLogService activityLogService() {
            return new ActivityLogService(null) {
                @Override
                public void log(String action, String actor) {
                }
            };
        }
    }

    static final class StubGeocodingService implements GeocodingService {

        private final Map<String, Coordinates> coordinatesByLocation = new ConcurrentHashMap<>();

        StubGeocodingService() {
            coordinatesByLocation.put("Bonifacio Global City", new Coordinates(14.5507, 121.0505));
            coordinatesByLocation.put("Ortigas Center", new Coordinates(14.5869, 121.0614));
        }

        @Override
        public Coordinates geocode(String location) {
            Coordinates coordinates = coordinatesByLocation.get(location);
            if (coordinates == null) {
                throw new IllegalArgumentException("No stubbed coordinates for location: " + location);
            }
            return coordinates;
        }
    }
}
