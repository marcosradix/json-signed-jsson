package org.jsson.spring.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.TreeMap;

/**
 * Core engine for strict JSSON Canonicalization.
 * Prepares strings deterministically for hash integrity.
 */
public class JssonC {

    private static final ObjectMapper canonicalMapper;

    static {
        // Initializes the mapper using the modern Jackson builder to avoid deprecation on MapperFeature modifications
        canonicalMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
        
        // Golden Rule 1: Forces alphabetical sorting of map keys globally
        canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        
        // Golden Rule 2: Removes spaces and formatting (minification)
        canonicalMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }

    /**
     * Transforms any object into a Canonical JSON String (JSSON-C)
     * @param input Target object for manipulation.
     * @return persistent and clean representation.
     */
    public static String canonicalize(Object input) throws JsonProcessingException {
        // We always convert to a sorted Map (TreeMap) to guarantee recursive sorting in mixed outputs final
        Object sortedObject = canonicalMapper.convertValue(input, TreeMap.class);
        String result = canonicalMapper.writeValueAsString(sortedObject);
        System.out.println("JSSON DEBUG Canonicalized: " + result);
        return result;
    }

    /**
     * Used by middlewares in the Spring ecosystem: prepares the purified received payload to sign or validate.
     * Isolates the $jsson node from the actual cryptographic mesh, since it is the "wax of the seal" itself
     * 
     * @param rawJson Raw input or output JSON.
     * @return Canonical string ready for strict Ed25519 hash validation.
     */
    public static String prepareForVerification(String rawJson) throws JsonProcessingException {
        ObjectNode node = (ObjectNode) canonicalMapper.readTree(rawJson);
        node.remove("$jsson"); // Identity proof never makes part of the block being proven
        return canonicalize(node);
    }
}
