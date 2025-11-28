package com.tecsup.productivity.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para diagnóstico de red
 * Útil para debugging de problemas de DNS
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostic")
public class NetworkDiagnosticController {

    private static final String GEMINI_HOST = "generativelanguage.googleapis.com";
    private static final String[] TEST_HOSTS = {
            "google.com",
            "8.8.8.8",
            GEMINI_HOST
    };

    /**
     * GET /api/diagnostic/network
     *
     * Prueba la conectividad de red y resolución DNS
     */
    @GetMapping("/network")
    public ResponseEntity<Map<String, Object>> checkNetwork() {
        log.info("🔍 Ejecutando diagnóstico de red...");

        Map<String, Object> results = new HashMap<>();
        Map<String, Object> dnsTests = new HashMap<>();

        // Test de DNS para cada host
        for (String host : TEST_HOSTS) {
            Map<String, Object> testResult = testDNS(host);
            dnsTests.put(host, testResult);
        }

        results.put("dns_tests", dnsTests);
        results.put("java_version", System.getProperty("java.version"));
        results.put("os_name", System.getProperty("os.name"));

        // Información de red del sistema
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            results.put("local_host", localhost.getHostAddress());
            results.put("local_hostname", localhost.getHostName());
        } catch (UnknownHostException e) {
            results.put("local_host", "Unable to resolve");
        }

        // Verificar variables de entorno de proxy
        results.put("http_proxy", System.getenv("HTTP_PROXY"));
        results.put("https_proxy", System.getenv("HTTPS_PROXY"));
        results.put("no_proxy", System.getenv("NO_PROXY"));

        log.info("✅ Diagnóstico completado");
        return ResponseEntity.ok(results);
    }

    /**
     * Prueba la resolución DNS de un host
     */
    private Map<String, Object> testDNS(String hostname) {
        Map<String, Object> result = new HashMap<>();
        result.put("hostname", hostname);

        long startTime = System.currentTimeMillis();

        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("resolution_time_ms", duration);
            result.put("ip_count", addresses.length);

            String[] ips = new String[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                ips[i] = addresses[i].getHostAddress();
            }
            result.put("ip_addresses", ips);

            log.info("✅ DNS OK para {}: {} ({} ms)", hostname, ips[0], duration);

        } catch (UnknownHostException e) {
            long duration = System.currentTimeMillis() - startTime;

            result.put("success", false);
            result.put("resolution_time_ms", duration);
            result.put("error", e.getMessage());

            log.error("❌ DNS FAIL para {}: {} ({} ms)", hostname, e.getMessage(), duration);
        }

        return result;
    }

    /**
     * GET /api/diagnostic/gemini
     *
     * Información específica sobre la configuración de Gemini
     */
    @GetMapping("/gemini")
    public ResponseEntity<Map<String, Object>> checkGemini() {
        Map<String, Object> info = new HashMap<>();

        // Solo información no sensible
        info.put("api_host", GEMINI_HOST);
        info.put("api_key_configured", System.getenv("GEMINI_API_KEY") != null);

        // Test de DNS específico para Gemini
        Map<String, Object> dnsTest = testDNS(GEMINI_HOST);
        info.put("dns_status", dnsTest);

        return ResponseEntity.ok(info);
    }
}