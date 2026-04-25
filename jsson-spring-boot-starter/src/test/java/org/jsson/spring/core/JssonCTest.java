package org.jsson.spring.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JssonCTest {

    @Test
    public void mustFollowJssonCStrictOrderingAndMinification() throws JsonProcessingException {
        // We create a messy map with unordered keys
        // Array and accents included as well
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("service", "5G Unlimited");
        innerMap.put("id", 99);

        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("status", "active");
        inputMap.put("details", innerMap);
        inputMap.put("z-index", 1);
        inputMap.put("app_list", Arrays.asList("WhatsApp", "Netflix", "Spotify"));
        inputMap.put("value", 29.9);
        inputMap.put("A-key", "First");

        // Canonical result strictly required by RFC (JSSON-C Strict)
        // 1. Alphabetical Order (A-key, details, app_list, status, value, z-index).
        // 2. Nested keys sorted (id, service).
        // 3. Floats in string base format and without spaces ("minified")
        String expected = "{\"A-key\":\"First\",\"app_list\":[\"WhatsApp\",\"Netflix\",\"Spotify\"],\"details\":{\"id\":99,\"service\":\"5G Unlimited\"},\"status\":\"active\",\"value\":29.9,\"z-index\":1}";
        
        String result = JssonC.canonicalize(inputMap);
        
        assertEquals(expected, result, "Canonicalization (JSSON-C) failed on ordering or master formatting!");
    }
}
