package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CompleteRegisterRequest;
import com.tecsup.productivity.dto.request.LoginRequest;
import com.tecsup.productivity.dto.request.ValidateRegisterRequest;
import com.tecsup.productivity.dto.response.AuthResponse;
import com.tecsup.productivity.dto.response.UserResponse;
import com.tecsup.productivity.dto.response.ValidationResponse;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.Habit;
import com.tecsup.productivity.entity.Task;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.repository.HabitRepository;
import com.tecsup.productivity.repository.TaskRepository;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AuthService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TaskRepository taskRepository; // ✅ AGREGADO
    private final HabitRepository habitRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    // ============================================
    // ✅ PASO 1: Validar sin crear usuario
    // ============================================
    @Transactional(readOnly = true)
    public ValidationResponse validateRegistration(ValidateRegisterRequest request) {

        log.info("🔍 Validando datos de registro...");

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }

        if (!request.getAcceptTerms()) {
            throw new BadRequestException("Debe aceptar los términos y condiciones");
        }

        if (request.getTipo() == User.UserType.STUDENT) {
            if (request.getTecsupToken() == null || request.getTecsupToken().isBlank()) {
                throw new BadRequestException(
                        "El token TECSUP es obligatorio para estudiantes"
                );
            }

            String token = request.getTecsupToken().trim();
            log.info("🔍 Validando token TECSUP...");

            if (!validateTecsupToken(token)) {
                throw new BadRequestException(
                        "Token TECSUP inválido o expirado. Verifica tu token de Canvas."
                );
            }

            log.info("✅ Token TECSUP válido");
        }

        log.info("✅ Validación exitosa para: {}", request.getEmail());

        return ValidationResponse.builder()
                .valid(true)
                .message("Datos validados correctamente")
                .build();
    }

    // ============================================
    // ✅ PASO 2: Completar registro con preferencias
    // ============================================
    @Transactional
    public AuthResponse completeRegistration(CompleteRegisterRequest request) {

        log.info("✅ Completando registro para: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }

        if (request.getPreferences() == null || request.getPreferences().isEmpty()) {
            throw new BadRequestException("Las preferencias son requeridas");
        }

        String validatedToken = null;

        if (request.getTipo() == User.UserType.STUDENT) {
            if (request.getTecsupToken() == null || request.getTecsupToken().isBlank()) {
                throw new BadRequestException(
                        "El token TECSUP es obligatorio para estudiantes."
                );
            }

            validatedToken = request.getTecsupToken().trim();

            log.info("🔍 Validando token TECSUP...");

            if (!validateTecsupToken(validatedToken)) {
                throw new BadRequestException(
                        "Token TECSUP inválido o expirado. Genera un nuevo token desde Canvas."
                );
            }

            log.info("✅ Token TECSUP válido");
        }

        // Crear usuario
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .tipo(request.getTipo())
                .tecsupToken(validatedToken)
                .preferences(request.getPreferences())
                .build();

        user = userRepository.save(user);
        log.info("✅ Usuario creado: {} ({})", user.getEmail(), user.getTipo());

        // ============================================
        // ✅ Crear hábitos por defecto
        // ============================================
        createDefaultHabits(user);

        // ============================================
        // ✅ CORREGIDO - Sincronizar EVENTOS + TAREAS
        // ============================================
        if (user.getTipo() == User.UserType.STUDENT && validatedToken != null) {
            try {
                log.info("🔄 Sincronizando datos de Canvas (eventos + tareas)...");
                Map<String, Integer> syncResult = fetchAndSyncTecsupData(user, validatedToken);

                int totalEvents = syncResult.get("events");
                int totalTasks = syncResult.get("tasks");

                log.info("✅ Sincronización completada: {} eventos, {} tareas",
                        totalEvents, totalTasks);
            } catch (Exception e) {
                log.warn("⚠️ Error al sincronizar datos de TECSUP: {}", e.getMessage());
            }
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
    }

    // ============================================
    // Registro directo (1 paso) - Mantener para compatibilidad
    // ============================================
    @Transactional
    public AuthResponse register(ValidateRegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }

        if (!request.getAcceptTerms()) {
            throw new BadRequestException("Debe aceptar los términos y condiciones");
        }

        String validatedToken = null;

        if (request.getTipo() == User.UserType.STUDENT) {
            if (request.getTecsupToken() == null || request.getTecsupToken().isBlank()) {
                throw new BadRequestException(
                        "El token TECSUP es obligatorio para estudiantes. " +
                                "Obtén tu token en Canvas: Settings → Approved Integrations → New Access Token"
                );
            }

            String token = request.getTecsupToken().trim();
            log.info("🔍 Validando token TECSUP para estudiante: {}", request.getEmail());

            if (!validateTecsupToken(token)) {
                throw new BadRequestException(
                        "Token TECSUP inválido o expirado. " +
                                "Verifica que copiaste correctamente el token desde Canvas."
                );
            }

            validatedToken = token;
            log.info("✅ Token TECSUP válido para: {}", request.getEmail());
        } else if (request.getTecsupToken() != null && !request.getTecsupToken().isBlank()) {
            log.warn("⚠️ Usuario GENERAL intentó registrarse con token TECSUP, se ignorará");
            validatedToken = null;
        }

        Map<String, Object> preferences = request.getPreferences();
        if (preferences == null || preferences.isEmpty()) {
            preferences = new HashMap<>();
            preferences.put("chatEnabled", true);
            preferences.put("darkMode", false);
            preferences.put("notifications", true);
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .tipo(request.getTipo())
                .tecsupToken(validatedToken)
                .preferences(preferences)
                .build();

        user = userRepository.save(user);
        log.info("✅ Usuario registrado exitosamente: {} ({})",
                user.getEmail(), user.getTipo());

        // ============================================
        // ✅ Crear hábitos por defecto
        // ============================================
        createDefaultHabits(user);

        // ============================================
        // ✅ CORREGIDO - Sincronizar EVENTOS + TAREAS
        // ============================================
        if (user.getTipo() == User.UserType.STUDENT && validatedToken != null) {
            try {
                log.info("🔄 Sincronizando datos de Canvas (eventos + tareas)...");
                Map<String, Integer> syncResult = fetchAndSyncTecsupData(user, validatedToken);

                int totalEvents = syncResult.get("events");
                int totalTasks = syncResult.get("tasks");

                log.info("✅ Sincronización automática completada: {} eventos, {} tareas",
                        totalEvents, totalTasks);
            } catch (Exception e) {
                log.warn("⚠️ No se pudieron sincronizar datos durante el registro: {}",
                        e.getMessage());
            }
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
    }

    // ============================================
    // Login (sin cambios)
    // ============================================
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
    }

    // ============================================
    // ✅ Crear hábitos por defecto
    // ============================================
    private void createDefaultHabits(User user) {
        log.info("🍽️ Creando hábitos por defecto para: {}", user.getEmail());

        List<Habit> defaultHabits = Arrays.asList(
                // Comidas (esComida = true, sin metaDiaria)
                Habit.builder()
                        .user(user)
                        .nombre("Desayuno")
                        .tipo(Habit.HabitType.DESAYUNO)
                        .esComida(true)
                        .metaDiaria(null)
                        .activo(true)
                        .build(),

                Habit.builder()
                        .user(user)
                        .nombre("Almuerzo")
                        .tipo(Habit.HabitType.ALMUERZO)
                        .esComida(true)
                        .metaDiaria(null)
                        .activo(true)
                        .build(),

                Habit.builder()
                        .user(user)
                        .nombre("Cena")
                        .tipo(Habit.HabitType.CENA)
                        .esComida(true)
                        .metaDiaria(null)
                        .activo(true)
                        .build(),

                // Hábitos regulares (esComida = false, con metaDiaria)
                Habit.builder()
                        .user(user)
                        .nombre("Agua")
                        .tipo(Habit.HabitType.AGUA)
                        .esComida(false)
                        .metaDiaria(8) // 8 vasos por día
                        .activo(true)
                        .build(),

                Habit.builder()
                        .user(user)
                        .nombre("Ejercicio")
                        .tipo(Habit.HabitType.EJERCICIO)
                        .esComida(false)
                        .metaDiaria(30) // 30 minutos por día
                        .activo(true)
                        .build(),

                Habit.builder()
                        .user(user)
                        .nombre("Sueño")
                        .tipo(Habit.HabitType.SUEÑO)
                        .esComida(false)
                        .metaDiaria(8) // 8 horas por día
                        .activo(true)
                        .build()
        );

        habitRepository.saveAll(defaultHabits);
        log.info("✅ {} hábitos creados para: {}", defaultHabits.size(), user.getEmail());
    }

    // ============================================
    // ✅ NUEVO MÉTODO - Sincronizar EVENTOS + TAREAS (igual que TecsupSyncService)
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
                                        .descripcion("Tarea de " + courseName) // ✅ CORREGIDO - Solo nombre del curso
                                        .fechaLimite(LocalDate.parse(dueDate.substring(0, 10)))
                                        .prioridad(Task.TaskPriority.MEDIA)
                                        .completed(false)
                                        .source("tecsup")
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
                            String description = (String) ev.get("description");

                            if (start != null && !start.isBlank()) {
                                LocalDate fecha = LocalDate.parse(start.substring(0, 10));
                                LocalTime hora;
                                try {
                                    hora = LocalTime.parse(start.substring(11, 16));
                                } catch (Exception ex) {
                                    hora = LocalTime.of(0, 0);
                                }

                                Event.EventCategory categoria = Event.EventCategory.CLASE;

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
                                        .descripcion(description != null ?
                                                description :
                                                "Evento de " + courseName)
                                        .source("tecsup")
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
            log.warn("❌ Token TECSUP inválido: {} - {}",
                    e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("❌ Error al validar token TECSUP", e);
            return false;
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .tipo(user.getTipo())
                .tecsupToken(user.getTecsupToken())
                .preferences(user.getPreferences())
                .createdAt(user.getCreatedAt())
                .build();
    }
}