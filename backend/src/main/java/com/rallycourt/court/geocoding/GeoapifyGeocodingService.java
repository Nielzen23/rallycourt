package com.rallycourt.court.geocoding;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GeoapifyGeocodingService implements GeocodingService {

    private final RestClient restClient;
    private final String apiKey;

    public GeoapifyGeocodingService(@Value("${app.geocoding.geoapify.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.geoapify.com/v1/geocode")
                .build();
        this.apiKey = apiKey;
    }

    @Override
    public Coordinates geocode(String location) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("Geoapify API key is not configured");
        }

        GeoapifyResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("text", location)
                        .queryParam("apiKey", apiKey)
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                    throw new ResponseStatusException(
                            clientResponse.getStatusCode(),
                            "Geoapify geocoding request failed"
                    );
                })
                .body(GeoapifyResponse.class);

        if (response == null || response.features() == null || response.features().isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Unable to geocode court location");
        }

        GeoapifyFeature feature = response.features().getFirst();
        if (feature.properties() == null) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400), "Unable to geocode court location");
        }

        return new Coordinates(feature.properties().lat(), feature.properties().lon());
    }

    private record GeoapifyResponse(List<GeoapifyFeature> features) {
    }

    private record GeoapifyFeature(GeoapifyProperties properties) {
    }

    private record GeoapifyProperties(double lat, double lon) {
    }
}
