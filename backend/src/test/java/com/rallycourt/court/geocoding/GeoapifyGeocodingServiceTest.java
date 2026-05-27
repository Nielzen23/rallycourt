package com.rallycourt.court.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.server.ResponseStatusException;

class GeoapifyGeocodingServiceTest {

    @Test
    void geocodeRejectsMissingApiKey() {
        GeoapifyGeocodingService service = new GeoapifyGeocodingService("", "ph");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.geocode("Makati City"));

        assertEquals("Geoapify API key is not configured", exception.getMessage());
    }

    @Test
    void geocodeRejectsNullResponse() {
        GeoapifyGeocodingService service = serviceWithResponse(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.geocode("Makati City")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Unable to geocode court location", exception.getReason());
    }

    @Test
    void geocodeRejectsEmptyFeatures() throws Exception {
        Object response = newGeoapifyResponse(List.of());
        GeoapifyGeocodingService service = serviceWithResponse(response);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.geocode("Makati City")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Unable to geocode court location", exception.getReason());
    }

    @Test
    void geocodeRejectsNullFeatures() throws Exception {
        Object response = newGeoapifyResponse(null);
        GeoapifyGeocodingService service = serviceWithResponse(response);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.geocode("Makati City")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Unable to geocode court location", exception.getReason());
    }

    @Test
    void geocodeRejectsMissingFeatureProperties() throws Exception {
        Object feature = newGeoapifyFeature(null);
        Object response = newGeoapifyResponse(List.of(feature));
        GeoapifyGeocodingService service = serviceWithResponse(response);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.geocode("Makati City")
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Unable to geocode court location", exception.getReason());
    }

    @Test
    void geocodeReturnsCoordinatesFromResponse() throws Exception {
        Object properties = newGeoapifyProperties(14.5547, 121.0244);
        Object feature = newGeoapifyFeature(properties);
        Object response = newGeoapifyResponse(List.of(feature));
        GeoapifyGeocodingService service = serviceWithResponse(response);

        Coordinates coordinates = service.geocode("Makati City");

        assertEquals(14.5547, coordinates.latitude());
        assertEquals(121.0244, coordinates.longitude());
    }

    @Test
    void geocodeBuildsExpectedGeoapifySearchUri() throws Exception {
        Object properties = newGeoapifyProperties(14.5547, 121.0244);
        Object feature = newGeoapifyFeature(properties);
        Object response = newGeoapifyResponse(List.of(feature));
        GeoapifyServiceMocks mocks = serviceMocks(response);

        mocks.service.geocode("Makati City");

        @SuppressWarnings("unchecked")
        Function<UriBuilder, ?> uriFunction = mocks.uriFunctionCaptor.getValue();
        UriBuilder uriBuilder = Mockito.mock(UriBuilder.class);
        when(uriBuilder.path("/search")).thenReturn(uriBuilder);
        when(uriBuilder.queryParam("text", "Makati City")).thenReturn(uriBuilder);
        when(uriBuilder.queryParam("apiKey", "test-api-key")).thenReturn(uriBuilder);
        when(uriBuilder.queryParam("limit", 1)).thenReturn(uriBuilder);
        when(uriBuilder.queryParamIfPresent("filter", java.util.Optional.of("countrycode:ph"))).thenReturn(uriBuilder);
        URI builtUri = URI.create("https://example.test/search");
        when(uriBuilder.build()).thenReturn(builtUri);

        Object result = uriFunction.apply(uriBuilder);

        assertSame(builtUri, result);
        verify(uriBuilder).path("/search");
        verify(uriBuilder).queryParam("text", "Makati City");
        verify(uriBuilder).queryParam("apiKey", "test-api-key");
        verify(uriBuilder).queryParam("limit", 1);
        verify(uriBuilder).queryParamIfPresent("filter", java.util.Optional.of("countrycode:ph"));
        verify(uriBuilder).build();
    }

    @Test
    void geocodeThrowsWhenGeoapifyReturnsHttpError() {
        GeoapifyServiceMocks mocks = serviceMocks(null);
        when(mocks.responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Predicate<HttpStatusCode> statusPredicate = invocation.getArgument(0);
            RestClient.ResponseSpec.ErrorHandler errorHandler = invocation.getArgument(1);
            HttpStatusCode statusCode = HttpStatusCode.valueOf(502);

            if (statusPredicate.test(statusCode)) {
                RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse clientResponse =
                        Mockito.mock(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse.class);
                when(clientResponse.getStatusCode()).thenReturn(statusCode);
                errorHandler.handle(null, clientResponse);
            }
            return mocks.responseSpec;
        });

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> mocks.service.geocode("Makati City")
        );

        assertEquals(502, exception.getStatusCode().value());
        assertEquals("Geoapify geocoding request failed", exception.getReason());
    }

    @Test
    void autocompleteReturnsEmptyListForBlankQuery() {
        GeoapifyGeocodingService service = new GeoapifyGeocodingService("test-api-key", "ph");

        assertEquals(List.of(), service.autocomplete("   "));
    }

    @Test
    void autocompleteReturnsFormattedSuggestions() throws Exception {
        Object first = newGeoapifyAutocompleteFeature("Makati City", 14.5547, 121.0244);
        Object second = newGeoapifyAutocompleteFeature("Ortigas Center", 14.5869, 121.0614);
        Object response = newGeoapifyResponse(List.of(first, second));
        GeoapifyGeocodingService service = serviceWithResponse(response);

        var suggestions = service.autocomplete("City");

        assertEquals(2, suggestions.size());
        assertEquals("Makati City", suggestions.getFirst().address());
        assertEquals(14.5547, suggestions.getFirst().latitude());
        assertEquals(121.0244, suggestions.getFirst().longitude());
    }

    @Test
    void autocompleteIgnoresFeaturesWithoutFormattedAddress() throws Exception {
        Class<?> propertiesClass = Class.forName("com.rallycourt.court.geocoding.GeoapifyGeocodingService$GeoapifyProperties");
        Constructor<?> constructor = propertiesClass.getDeclaredConstructor(double.class, double.class, String.class);
        constructor.setAccessible(true);
        Object properties = constructor.newInstance(14.5547, 121.0244, null);
        Object response = newGeoapifyResponse(List.of(newGeoapifyFeature(properties)));
        GeoapifyGeocodingService service = serviceWithResponse(response);

        assertEquals(List.of(), service.autocomplete("City"));
    }

    @Test
    void autocompleteReturnsEmptyListForNullFeatures() throws Exception {
        GeoapifyGeocodingService service = serviceWithResponse(newGeoapifyResponse(null));

        assertEquals(List.of(), service.autocomplete("City"));
    }

    private GeoapifyGeocodingService serviceWithResponse(Object response) {
        return serviceMocks(response).service;
    }

    private GeoapifyServiceMocks serviceMocks(Object response) {
        GeoapifyGeocodingService service = new GeoapifyGeocodingService("test-api-key", "ph");
        RestClient restClient = Mockito.mock(RestClient.class);
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = Mockito.mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = Mockito.mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = Mockito.mock(RestClient.ResponseSpec.class);
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Function<UriBuilder, ?>> uriFunctionCaptor =
                org.mockito.ArgumentCaptor.forClass(Function.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(uriFunctionCaptor.capture())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        doAnswer(invocation -> response).when(responseSpec).body(any(Class.class));

        ReflectionTestUtils.setField(service, "restClient", restClient);
        return new GeoapifyServiceMocks(service, responseSpec, uriFunctionCaptor);
    }

    private Object newGeoapifyResponse(List<?> features) throws Exception {
        Class<?> responseClass = Class.forName("com.rallycourt.court.geocoding.GeoapifyGeocodingService$GeoapifyResponse");
        Constructor<?> constructor = responseClass.getDeclaredConstructor(List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(features);
    }

    private Object newGeoapifyFeature(Object properties) throws Exception {
        Class<?> featureClass = Class.forName("com.rallycourt.court.geocoding.GeoapifyGeocodingService$GeoapifyFeature");
        Class<?> propertiesClass = Class.forName("com.rallycourt.court.geocoding.GeoapifyGeocodingService$GeoapifyProperties");
        Constructor<?> constructor = featureClass.getDeclaredConstructor(propertiesClass);
        constructor.setAccessible(true);
        return constructor.newInstance(properties);
    }

    private Object newGeoapifyProperties(double lat, double lon) throws Exception {
        Class<?> propertiesClass = Class.forName("com.rallycourt.court.geocoding.GeoapifyGeocodingService$GeoapifyProperties");
        Constructor<?> constructor = propertiesClass.getDeclaredConstructor(double.class, double.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(lat, lon, "Formatted address");
    }

    private Object newGeoapifyAutocompleteFeature(String formatted, double lat, double lon) throws Exception {
        Class<?> propertiesClass = Class.forName("com.rallycourt.court.geocoding.GeoapifyGeocodingService$GeoapifyProperties");
        Constructor<?> constructor = propertiesClass.getDeclaredConstructor(double.class, double.class, String.class);
        constructor.setAccessible(true);
        Object properties = constructor.newInstance(lat, lon, formatted);
        return newGeoapifyFeature(properties);
    }

    private record GeoapifyServiceMocks(
            GeoapifyGeocodingService service,
            RestClient.ResponseSpec responseSpec,
            org.mockito.ArgumentCaptor<Function<UriBuilder, ?>> uriFunctionCaptor
    ) {
    }
}
