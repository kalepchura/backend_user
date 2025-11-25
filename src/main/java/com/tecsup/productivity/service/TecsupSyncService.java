package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.SyncTecsupRequest;
import com.tecsup.productivity.dto.response.SyncResponse;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.repository.TaskRepository;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.util.SecurityUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TecsupSyncService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    // ============================================
    // ✅ Habilitar sincronización TECSUP
    // ============================================
    @Transactional
    public SyncResponse enableSync(SyncTecsupRequest request) {

        User user = securityUtil.getCurrentUser();
        if (user == null) {
            throw new BadRequestException("Usuario no autenticado");
        }

        log.info("[SYNC] Habilitando sincronización para: {}", user.getEmail());

        if (user.getTipo() != User.UserType.STUDENT) {
            throw new BadRequestException("Solo estudiantes pueden sincronizar con TECSUP");
        }

        String token = request.getToken();
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Token TECSUP inválido");
        }

        // 1️⃣ Validar token contra Canvas API
        if (!validateTecsupToken(token)) {
            throw new BadRequestException("Token TECSUP inválido o expirado");
        }

        // 2️⃣ Guardar token
        user.setTecsupToken(token.trim());

        // 3️⃣ Actualizar preferences.sync.tecsup = true
        Map<String, Object> preferences = user.getPreferences();
        if (preferences == null) {
            preferences = new HashMap<>();
        }

        Map<String, Object> sync = new HashMap<>();
        sync.put("tecsup", true);
        sync.put("lastSyncAt", java.time.LocalDateTime.now().toString());
        preferences.put("sync", sync);

        user.setPreferences(preferences);
        userRepository.save(user);

        log.info("[SYNC] Token guardado y preferences actualizadas");

        // 4️⃣ Importar datos desde Canvas
        Map<String, Integer> syncResult = fetchAndSyncTecsupData(user, token);

        int totalEvents = syncResult.get("events");
        int totalTasks = syncResult.get("tasks");

        log.info("[SYNC] ✅ Sincronización completada: {} eventos, {} tareas",
                totalEvents, totalTasks);

        return SyncResponse.builder()
                .eventosSincronizados(totalEvents)
                .tareasSincronizadas(totalTasks)
                .mensaje(String.format("✅ %d eventos y %d tareas sincronizadas desde TECSUP",
                        totalEvents, totalTasks))
                .build();
    }

    // ============================================
    // ✅ Deshabilitar sincronización TECSUP
    // ============================================
    @Transactional
    public void disableSync() {

        User user = securityUtil.getCurrentUser();
        if (user == null) {
            throw new BadRequestException("Usuario no autenticado");
        }

        log.info("[SYNC] Deshabilitando sincronización para: {}", user.getEmail());

        // 1️⃣ Eliminar SOLO datos con source="tecsup"
        eventRepository.deleteByUserIdAndSource(user.getId(), "tecsup");
        taskRepository.deleteByUserIdAndSource(user.getId(), "tecsup");

        log.info("[SYNC] Datos TECSUP eliminados (eventos y tareas)");

        // 2️⃣ Limpiar token
        user.setTecsupToken(null);

        // 3️⃣ Actualizar preferences.sync.tecsup = false
        Map<String, Object> preferences = user.getPreferences();
        if (preferences != null) {
            Map<String, Object> sync = (Map<String, Object>) preferences.getOrDefault("sync", new HashMap<>());
            sync.put("tecsup", false);
            sync.put("lastSyncAt", null);
            preferences.put("sync", sync);
            user.setPreferences(preferences);
        }

        userRepository.save(user);

        log.info("[SYNC] ✅ Sincronización deshabilitada. Datos locales preservados.");
    }

    // ============================================
    // ✅ Re-sincronizar (refrescar datos)
    // ============================================
    @Transactional
    public SyncResponse refreshSync() {

        User user = securityUtil.getCurrentUser();
        if (user == null) {
            throw new BadRequestException("Usuario no autenticado");
        }

        String token = user.getTecsupToken();
        if (token == null || token.isBlank()) {
            throw new BadRequestException("No hay token TECSUP guardado");
        }

        log.info("[SYNC] Re-sincronizando datos para: {}", user.getEmail());

        // 1️⃣ Eliminar SOLO datos de TECSUP
        eventRepository.deleteByUserIdAndSource(user.getId(), "tecsup");
        taskRepository.deleteByUserIdAndSource(user.getId(), "tecsup");

        // 2️⃣ Volver a importar
        Map<String, Integer> syncResult = fetchAndSyncTecsupData(user, token);

        // 3️⃣ Actualizar lastSyncAt
        Map<String, Object> preferences = user.getPreferences();
        Map<String, Object> sync = (Map<String, Object>) preferences.getOrDefault("sync", new HashMap<>());
        sync.put("lastSyncAt", java.time.LocalDateTime.now().toString());
        preferences.put("sync", sync);
        user.setPreferences(preferences);
        userRepository.save(user);

        int totalEvents = syncResult.get("events");
        int totalTasks = syncResult.get("tasks");

        log.info("[SYNC] ✅ Re-sincronización completada: {} eventos, {} tareas",
                totalEvents, totalTasks);

        return SyncResponse.builder()
                .eventosSincronizados(totalEvents)
                .tareasSincronizadas(totalTasks)
                .mensaje(String.format("✅ %d eventos y %d tareas actualizadas",
                        totalEvents, totalTasks))
                .build();
    }

    // ============================================
    // ✅ MÉTODO PRIVADO - Importar datos desde Canvas
    // ============================================
    private Map<String, Integer> fetchAndSyncTecsupData(User user, String token) {

        String baseUrl = "https://tecsup.instructure.com/api/v1";
        RestTemplate rest = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<Event> eventos = new ArrayList<>();
        List<Task> tareas = new ArrayList<>();

        try {
            // 1️⃣ Obtener cursos
            ResponseEntity<List> courseResp = rest.exchange(
                    baseUrl + "/courses",
                    HttpMethod.GET,
                    entity,
                    List.class
            );

            List<Map<String, Object>> cursos = courseResp.getBody();
            if (cursos == null) {
                log.warn("[SYNC] No se encontraron cursos");
                return Map.of("events", 0, "tasks", 0);
            }

            for (Map<String, Object> curso : cursos) {

                Integer courseId = (Integer) curso.get("id");
                String courseName = (String) curso.get("name");

                log.info("[SYNC] Procesando curso: {} | {}", courseId, courseName);

                // ============================================
                // ✅ 2️⃣ Canvas /assignments → Task entity
                // ============================================
                try {
                    ResponseEntity<List> assignmentsResp = rest.exchange(
                            baseUrl + "/courses/" + courseId + "/assignments",
                            HttpMethod.GET,
                            entity,
                            List.class
                    );

                    List<Map<String, Object>> assignments = assignmentsResp.getBody();
                    if (assignments != null) {
                        for (Map<String, Object> assignment : assignments) {
                            Integer assignmentId = (Integer) assignment.get("id");
                            String name = (String) assignment.get("name");
                            String dueDate = (String) assignment.get("due_at");
                            String description = (String) assignment.get("description");

                            if (dueDate != null && !dueDate.isBlank()) {
                                tareas.add(Task.builder()
                                        .user(user)
                                        .titulo(name)
                                        .descripcion(description != null ?
                                                description :
                                                "Tarea de " + courseName)
                                        .fechaLimite(LocalDate.parse(dueDate.substring(0, 10)))
                                        .prioridad(Task.TaskPriority.MEDIA) // Default local
                                        .completed(false)
                                        .source("tecsup") // ✅ Origen TECSUP
                                        .tecsupExternalId(String.valueOf(assignmentId))
                                        .sincronizadoTecsup(true)
                                        .build());
                            }
                        }
                    }
                } catch (HttpClientErrorException e) {
                    log.error("[SYNC] Error al obtener assignments del curso {}: {}",
                            courseId, e.getStatusCode());
                }

                // ============================================
                // ✅ 3️⃣ Canvas /calendar_events → Event entity
                // ============================================
                try {
                    ResponseEntity<List> calendarResp = rest.exchange(
                            baseUrl + "/calendar_events?context_codes[]=course_" + courseId,
                            HttpMethod.GET,
                            entity,
                            List.class
                    );

                    List<Map<String, Object>> calendarEvents = calendarResp.getBody();
                    if (calendarEvents != null) {
                        for (Map<String, Object> ev : calendarEvents) {
                            Integer eventId = (Integer) ev.get("id");
                            String title = (String) ev.get("title");
                            String start = (String) ev.get("start_at");

                            if (start != null && !start.isBlank()) {
                                LocalDate fecha = LocalDate.parse(start.substring(0, 10));
                                LocalTime hora;
                                try {
                                    hora = LocalTime.parse(start.substring(11, 16));
                                } catch (Exception ex) {
                                    hora = LocalTime.of(0, 0);
                                }

                                // ✅ Determinar categoría (CLASE por defecto)
                                Event.EventCategory categoria = Event.EventCategory.CLASE;

                                // Si el título contiene "examen", clasificar como EXAMEN
                                if (title != null &&
                                        (title.toLowerCase().contains("examen") ||
                                                title.toLowerCase().contains("exam") ||
                                                title.toLowerCase().contains("evaluación"))) {
                                    categoria = Event.EventCategory.EXAMEN;
                                }

                                eventos.add(Event.builder()
                                        .user(user)
                                        .titulo(title)
                                        .fecha(fecha)
                                        .hora(hora)
                                        .categoria(categoria)
                                        .curso(courseName)
                                        .descripcion("Tarea de " + courseName)
                                        .source("tecsup") // ✅ Origen TECSUP
                                        .tecsupExternalId(String.valueOf(eventId))
                                        .sincronizadoTecsup(true)
                                        .build());
                            }
                        }
                    }
                } catch (HttpClientErrorException e) {
                    log.error("[SYNC] Error al obtener calendar events del curso {}: {}",
                            courseId, e.getStatusCode());
                }
            }

        } catch (HttpClientErrorException e) {
            log.error("[SYNC] Error al obtener cursos: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Token TECSUP inválido o expirado");
        } catch (Exception e) {
            log.error("[SYNC] Error inesperado", e);
            throw new BadRequestException("Error al sincronizar con TECSUP");
        }

        // ============================================
        // ✅ 4️⃣ Guardar en BD
        // ============================================
        if (!eventos.isEmpty()) {
            eventRepository.saveAll(eventos);
            log.info("[SYNC] {} eventos guardados", eventos.size());
        }
        if (!tareas.isEmpty()) {
            taskRepository.saveAll(tareas);
            log.info("[SYNC] {} tareas guardadas", tareas.size());
        }

        return Map.of(
                "events", eventos.size(),
                "tasks", tareas.size()
        );
    }

    // ============================================
    // ✅ Validar token contra Canvas API
    // ============================================
    private boolean validateTecsupToken(String token) {
        try {
            String baseUrl = "https://tecsup.instructure.com/api/v1";
            RestTemplate rest = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = rest.exchange(
                    baseUrl + "/users/self",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getStatusCode() == HttpStatus.OK;

        } catch (HttpClientErrorException e) {
            log.warn("[SYNC] Token inválido: {}", e.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("[SYNC] Error al validar token", e);
            return false;
        }
    }
}