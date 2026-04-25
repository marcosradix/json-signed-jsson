package org.jsson.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jsson.security")
public record JssonProperties(
    String keystorePath,
    String keystorePassword,
    String keystoreType,
    String keyAlias,
    String keyPassword,
    String privateKeyPath,
    String publicKeyPath
) {
    public JssonProperties {
        if (keystoreType == null) {
            keystoreType = "PKCS12";
        }
    }
}
