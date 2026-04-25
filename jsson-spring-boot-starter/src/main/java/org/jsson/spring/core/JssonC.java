package org.jsson.spring.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * Core engine for strict JSSON Canonicalization.
 * Prepares strings deterministically for hash integrity.
 */
public class JssonC {

    private static final Logger logger = LoggerFactory.getLogger(JssonC.class);
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
        return canonicalize(input, null, null);
    }

    /**
     * Transforms any object into a Canonical JSON String (JSSON-C), filtering root fields
     * based on includes and excludes if provided.
     */
    public static String canonicalize(Object input, Set<String> includes, Set<String> excludes) throws JsonProcessingException {
        // We always convert to a sorted Map (TreeMap) to guarantee recursive sorting in mixed outputs final
        Object sortedObject = canonicalMapper.convertValue(input, TreeMap.class);
        
        if (sortedObject instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) sortedObject;
            if (includes != null && !includes.isEmpty()) {
                map.keySet().retainAll(includes);
            }
            if (excludes != null && !excludes.isEmpty()) {
                map.keySet().removeAll(excludes);
            }
        }

        String jsonResult = canonicalMapper.writeValueAsString(sortedObject);
        String boundary = buildBoundaryString(includes, excludes);
        
        String finalResult = jsonResult + boundary;
        logger.debug("JSSON Hash Input Generated: {}", finalResult);
        return finalResult;
    }

    public static String buildBoundaryString(Set<String> includes, Set<String> excludes) {
        StringBuilder sb = new StringBuilder();
        if (includes != null && !includes.isEmpty()) {
            sb.append("|inc:").append(new java.util.TreeSet<>(includes));
        } else {
            sb.append("|inc:all");
        }
        
        if (excludes != null && !excludes.isEmpty()) {
            sb.append("|exc:").append(new java.util.TreeSet<>(excludes));
        }
        return sb.toString();
    }

    /**
     * Used by middlewares in the Spring ecosystem: prepares the purified received payload to sign or validate.
     * Isolates the $jsson node from the actual cryptographic mesh, since it is the "wax of the seal" itself
     */
    public static String prepareForVerification(String rawJson) throws JsonProcessingException {
        ObjectNode node = (ObjectNode) canonicalMapper.readTree(rawJson);
        JsonNode proofNode = node.get("$jsson");
        
        Set<String> includes = new HashSet<>();
        Set<String> excludes = new HashSet<>();
        
        if (proofNode != null && proofNode.has("sig")) {
            String sigToken = proofNode.get("sig").asText();
            if (sigToken.contains(".")) {
                String encodedBoundary = sigToken.split("\\.")[0];
                String decodedBoundary = new String(java.util.Base64.getUrlDecoder().decode(encodedBoundary), java.nio.charset.StandardCharsets.UTF_8);
                parseBoundaryString(decodedBoundary, includes, excludes);
            }
        }

        node.remove("$jsson");
        return canonicalize(node, includes, excludes);
    }

    private static void parseBoundaryString(String boundary, Set<String> includes, Set<String> excludes) {
        if (boundary.startsWith("|inc:")) {
            int incEnd = boundary.indexOf("|exc:");
            String incPart = (incEnd == -1) ? boundary.substring(5) : boundary.substring(5, incEnd);
            if (!incPart.equals("all")) {
                // [field1, field2] -> field1, field2
                String cleaned = incPart.substring(1, incPart.length() - 1);
                if (!cleaned.isEmpty()) {
                    for (String s : cleaned.split(",")) {
                        includes.add(s.trim());
                    }
                }
            }
            if (incEnd != -1) {
                String excPart = boundary.substring(incEnd + 5);
                String cleaned = excPart.substring(1, excPart.length() - 1);
                if (!cleaned.isEmpty()) {
                    for (String s : cleaned.split(",")) {
                        excludes.add(s.trim());
                    }
                }
            }
        }
    }
}
