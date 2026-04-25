package org.jsson.spring.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    @Test
    public void mustRespectIncludesFilter() throws JsonProcessingException {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("status", "active");
        inputMap.put("value", 29.9);
        inputMap.put("A-key", "First");
        
        Set<String> includes = new HashSet<>(Arrays.asList("status", "value"));
        
        String expected = "{\"status\":\"active\",\"value\":29.9}";
        String result = JssonC.canonicalize(inputMap, includes, null);
        
        assertEquals(expected, result, "Canonicalization failed to apply includes filter!");
    }

    @Test
    public void mustRespectExcludesFilter() throws JsonProcessingException {
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("status", "active");
        inputMap.put("value", 29.9);
        inputMap.put("A-key", "First");
        
        Set<String> excludes = new HashSet<>(Arrays.asList("status"));
        
        String expected = "{\"A-key\":\"First\",\"value\":29.9}";
        String result = JssonC.canonicalize(inputMap, null, excludes);
        
        assertEquals(expected, result, "Canonicalization failed to apply excludes filter!");
    }

    @Test
    public void prepareForVerificationMustExtractIncAndExc() throws JsonProcessingException {
        String rawJson = "{\"A-key\":\"First\",\"status\":\"active\",\"value\":29.9,\"$jsson\":{\"inc\":[\"status\",\"value\"],\"sig\":\"test\"}}";
        
        String expectedCanonical = "{\"status\":\"active\",\"value\":29.9}";
        String result = JssonC.prepareForVerification(rawJson);
        
        assertEquals(expectedCanonical, result, "prepareForVerification failed to extract and apply 'inc' from $jsson!");
    }
}
