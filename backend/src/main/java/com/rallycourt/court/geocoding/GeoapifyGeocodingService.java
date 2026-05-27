package com.rallycourt.court.geocoding;

import com.rallycourt.court.dto.CourtAddressSuggestionResponse;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeoapifyGeocodingService implements GeocodingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoapifyGeocodingService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String countryCodeFilter;

    public GeoapifyGeocodingService(
            @Value("${app.geocoding.geoapify.api-key}") String apiKey,
            @Value("${app.geocoding.geoapify.country-code:ph}") String countryCodeFilter
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.geoapify.com/v1/geocode")
                .build();
        this.apiKey = apiKey;
        this.countryCodeFilter = normalizeCountryCode(countryCodeFilter);
    }

    @Override
    public Coordinates geocode(String location) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Geoapify API key is not configured");
        }
        LOGGER.info("Geoapify geocode requested for location length {}", location != null ? location.length() : 0);

        GeoapifyResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("text", location)
                        .queryParam("apiKey", apiKey)
                        .queryParam("limit", 1)
                        .queryParamIfPresent("filter", buildCountryFilter())
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    LOGGER.warn("Geoapify geocode request failed with status {}", clientResponse.getStatusCode());
                    throw new ResponseStatusException(
                            clientResponse.getStatusCode(),
                            "Geoapify geocoding request failed"
                    );
                })
                .body(GeoapifyResponse.class);

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.warn("Geoapify geocode returned no results");
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Unable to geocode court location");
        }

        GeoapifyFeature feature = response.features().getFirst();
        if (feature.properties() == null) {
            LOGGER.warn("Geoapify geocode returned feature without properties");
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Unable to geocode court location");
        }

        LOGGER.info("Geoapify geocode resolved coordinates successfully");
        return new Coordinates(feature.properties().lat(), feature.properties().lon());
    }

    @Override
    public List<CourtAddressSuggestionResponse> autocomplete(String query) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Geoapify API key is not configured");
        }

        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        LOGGER.info("Geoapify autocomplete requested for query length {}", query.length());

        GeoapifyResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/autocomplete")
                        .queryParam("text", query)
                        .queryParam("apiKey", apiKey)
                        .queryParam("limit", 5)
                        .queryParamIfPresent("filter", buildCountryFilter())
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    LOGGER.warn("Geoapify autocomplete request failed with status {}", clientResponse.getStatusCode());
                    throw new ResponseStatusException(
                            clientResponse.getStatusCode(),
                            "Geoapify autocomplete request failed"
                    );
                })
                .body(GeoapifyResponse.class);

        if (response == null || response.features() == null) {
            LOGGER.info("Geoapify autocomplete returned no features");
            return List.of();
        }

        LOGGER.info("Geoapify autocomplete returned {} features", response.features().size());
        return response.features().stream()
                .filter(feature -> feature.properties() != null && StringUtils.hasText(feature.properties().formatted()))
                .map(feature -> new CourtAddressSuggestionResponse(
                        feature.properties().formatted(),
                        feature.properties().lat(),
                        feature.properties().lon()
                ))
                .toList();
    }

    private String normalizeCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return "";
        }
        return countryCode.trim().toLowerCase(Locale.ROOT);
    }

    private java.util.Optional<String> buildCountryFilter() {
        if (!StringUtils.hasText(countryCodeFilter)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of("countrycode:" + countryCodeFilter);
    }

    private record GeoapifyResponse(List<GeoapifyFeature> features) {
    }

    private record GeoapifyFeature(GeoapifyProperties properties) {
    }

    private record GeoapifyProperties(double lat, double lon, String formatted) {
    }
}
