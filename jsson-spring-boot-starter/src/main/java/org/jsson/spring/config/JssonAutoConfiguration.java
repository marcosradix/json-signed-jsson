package org.jsson.spring.config;

import org.jsson.spring.interceptor.JssonRequestInterceptor;
import org.jsson.spring.interceptor.JssonResponseInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JssonProperties.class)
public class JssonAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "jsson.enabled", matchIfMissing = true)
    public JssonResponseInterceptor jssonResponseInterceptor(JssonProperties properties) {
        return new JssonResponseInterceptor(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "jsson.enabled", matchIfMissing = true)
    public JssonRequestInterceptor jssonRequestInterceptor(JssonProperties properties) {
        return new JssonRequestInterceptor(properties);
    }
}
