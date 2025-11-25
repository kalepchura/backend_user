package com.tecsup.productivity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "google.gemini.api")
public class GeminiProperties {
    private String key;
    private String url;
    private String model;
    private int maxTokens = 500;
    private double temperature = 0.7;
}
