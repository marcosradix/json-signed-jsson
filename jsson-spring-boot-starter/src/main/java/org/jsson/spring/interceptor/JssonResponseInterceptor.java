package org.jsson.spring.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsson.spring.annotation.JssonSign;
import org.jsson.spring.config.JssonProperties;
import org.jsson.spring.core.JssonC;
import org.jsson.spring.crypto.CryptoService;
import org.jsson.spring.crypto.KeyUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.annotation.PostConstruct;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Global Controller Advice acting on all Spring Boot Web responses.
 * Performs mathematical transformations using the Private Key extracted via Certificate (Corporate Keystore).
 */
@ControllerAdvice
public class JssonResponseInterceptor implements ResponseBodyAdvice<Object> {

    private static final Logger logger = LoggerFactory.getLogger(JssonResponseInterceptor.class);
    private final JssonProperties properties;
    private PrivateKey privateKey;

    public JssonResponseInterceptor(JssonProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() throws Exception {
        if (properties.keystorePath() != null && !properties.keystorePath().isEmpty()) {
            KeyStore ks = KeyUtil.loadKeyStore(properties.keystorePath(), properties.keystorePassword(), properties.keystoreType());
            String pwd = properties.keyPassword() != null ? properties.keyPassword() : properties.keystorePassword();
            privateKey = KeyUtil.getPrivateKey(ks, properties.keyAlias(), pwd);
        } else if (properties.privateKeyPath() != null && !properties.privateKeyPath().isEmpty()) {
            String pemContent = KeyUtil.readFileContent(properties.privateKeyPath());
            privateKey = KeyUtil.loadPrivateKeyFromPemOrBase64(pemContent);
        }
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        boolean hasMethodAnnotation = returnType.hasMethodAnnotation(JssonSign.class);
        boolean hasClassAnnotation = returnType.getDeclaringClass().isAnnotationPresent(JssonSign.class);
        return hasMethodAnnotation || hasClassAnnotation;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        try {
            if (this.privateKey == null) {
                throw new IllegalStateException("Missing signing Private Key from Certificate (Keystore). Check jsson.security variables!");
            }
            
            JssonSign jssonSign = returnType.getMethodAnnotation(JssonSign.class);
            if (jssonSign == null) {
                jssonSign = returnType.getDeclaringClass().getAnnotation(JssonSign.class);
            }
            
            Set<String> includes = new HashSet<>();
            Set<String> excludes = new HashSet<>();
            
            if (jssonSign != null) {
                if (jssonSign.includes().length > 0) {
                    includes.addAll(Arrays.asList(jssonSign.includes()));
                }
                if (jssonSign.excludes().length > 0) {
                    excludes.addAll(Arrays.asList(jssonSign.excludes()));
                }
            }

            // 1. Canonicalize and Sort (using selective boundaries if provided)
            String canonicalData = JssonC.canonicalize(body, includes, excludes);

            // 2. Sign (based on the certificate key)
            String rawSignature = CryptoService.sign(canonicalData, privateKey);
            
            // 3. Tokenize Signature (Pack boundary metadata into the sig string)
            String boundary = JssonC.buildBoundaryString(includes, excludes);
            String encodedBoundary = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(boundary.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String signatureToken = encodedBoundary + "." + rawSignature;

            // 4. Assemble Security Node
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode finalNode = mapper.valueToTree(body);
            ObjectNode proofNode = finalNode.putObject("$jsson");
            proofNode.put("v", "1");
            proofNode.put("alg", "Ed25519");
            proofNode.put("sig", signatureToken);

            return finalNode;
        } catch (Exception e) {
            throw new RuntimeException("JSSON - SYSTEM BOOT - Internal error sealing the JSSON package with X.509 certified key: ", e);
        }
    }
}
