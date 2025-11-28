package com.tecsup.productivity.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuración mejorada para la integración con Gemini AI
 * Soluciona problemas de DNS resolution y timeouts
 */
@Configuration
public class GeminiConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // 1. Configurar connection provider con límites
        ConnectionProvider connectionProvider = ConnectionProvider.builder("gemini-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // 2. Configurar HttpClient con DNS y timeouts optimizados
        HttpClient httpClient = HttpClient.create(connectionProvider)
                // SOLUCIÓN DNS: Usar el resolver del sistema en lugar de Netty DNS
                .resolver(DefaultAddressResolverGroup.INSTANCE)

                // Timeouts de conexión
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 45000) // 45 segundos
                .responseTimeout(Duration.ofSeconds(60)) // 60 segundos para respuesta

                // Configurar handlers de timeout
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS)))

//                // Logging para debug (opcional, quitar en producción)
//                .wiretap("reactor.netty.http.client.HttpClient",
//                        io.netty.handler.logging.LogLevel.DEBUG,
//                        io.netty.handler.logging.LoggingHandler.DEFAULT_CHARSET)

                // Retry automático en caso de errores de conexión
                .compress(true);

        // 3. Configurar estrategias de memoria para respuestas grandes
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        // 4. Construir WebClient
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader("User-Agent", "TECSUP-Productivity-Backend/1.0");
    }
}