package org.jsson.spring.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsson.spring.annotation.JssonVerify;
import org.jsson.spring.config.JssonProperties;
import org.jsson.spring.core.JssonC;
import org.jsson.spring.crypto.CryptoService;
import org.jsson.spring.crypto.KeyUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller Advice that intercepts payloads to validate based
 * on the Public Key originating from the operation's official Certificate (Keystore).
 */
@ControllerAdvice
public class JssonRequestInterceptor extends RequestBodyAdviceAdapter {

    private static final Logger logger = LoggerFactory.getLogger(JssonRequestInterceptor.class);
    private final JssonProperties properties;
    private final ObjectMapper basicMapper = new ObjectMapper();
    private PublicKey publicKey;

    public JssonRequestInterceptor(JssonProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws Exception {
        if (properties.keystorePath() != null && !properties.keystorePath().isEmpty()) {
            KeyStore ks = KeyUtil.loadKeyStore(properties.keystorePath(), properties.keystorePassword(), properties.keystoreType());
            publicKey = KeyUtil.getPublicKey(ks, properties.keyAlias());
        } else if (properties.publicKeyPath() != null && !properties.publicKeyPath().isEmpty()) {
            String pemContent = KeyUtil.readFileContent(properties.publicKeyPath());
            publicKey = KeyUtil.loadPublicKeyFromPemOrBase64(pemContent);
        }
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasParameterAnnotation(JssonVerify.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        String rawBody = new String(inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);
        
        try {
            if (publicKey == null) {
                throw new IllegalStateException("For @JssonVerify to run, the associated Public Key from the Certificate must be loaded!");
            }

            JsonNode rootNode = basicMapper.readTree(rawBody);
            JsonNode proofNode = rootNode.get("$jsson");
            if (proofNode == null || !proofNode.has("sig")) {
                throw new SecurityException("JSSON Body Malformed: Reserved $jsson field missing or incomplete.");
            }

            String signatureToken = proofNode.get("sig").asText();
            String rawSignature = signatureToken;
            
            // If it's a tokenized signature, extract the raw signature part (the second segment)
            if (signatureToken.contains(".")) {
                rawSignature = signatureToken.split("\\.")[1];
            }

            String canonicalData = JssonC.prepareForVerification(rawBody);
            
            if (!CryptoService.verify(canonicalData, rawSignature, publicKey)) {
                throw new SecurityException("JSSON Fraud Lock: Corrupted data or signature cryptographically violated via key mismatch.");
            }
            
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "403 Forbidden JSSON Breach - " + e.getMessage(), e);
        }
        
        // Returns the unmodified block back to Spring
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(rawBody.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                return inputMessage.getHeaders();
            }
        };
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body; 
    }
}
