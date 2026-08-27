package com.thiago.feedback_analyser.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiFeignConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Bean
    public RequestInterceptor geminiApiKeyInterceptor() {
        return requestTemplate -> requestTemplate.header("x-goog-api-key", apiKey);
    }

    @Bean
    public Logger.Level geminiFeignLoggerLevel() {

        return Logger.Level.BASIC;
    }
}
