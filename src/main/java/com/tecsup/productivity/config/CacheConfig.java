package com.tecsup.productivity.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de caché en memoria para el historial temporal del chatbot
 * El historial NO se persiste en BD, solo en memoria durante la sesión
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${chatbot.session.timeout:3600}")
    private int sessionTimeout;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("chatHistory");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(sessionTimeout, TimeUnit.SECONDS) // Expira después de 1 hora
                .maximumSize(1000)); // Máximo 1000 conversaciones en memoria
        return cacheManager;
    }
}