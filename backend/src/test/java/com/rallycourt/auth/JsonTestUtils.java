package com.rallycourt.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestUtils() {
    }

    static String extractToken(String json) throws Exception {
        JsonNode node = OBJECT_MAPPER.readTree(json);
        return node.get("token").asText();
    }
}
