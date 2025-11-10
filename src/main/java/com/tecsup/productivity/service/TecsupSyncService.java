// ============================================
// TecsupSyncService.java - EP-03 (HU-7, CA-10)
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.SyncTecsupRequest;
import com.tecsup.productivity.dto.response.EventResponse;
import com.tecsup.productivity.dto.response.SyncResponse;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TecsupSyncService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public SyncResponse syncTecsup(SyncTecsupRequest request) {
        User user = securityUtil.getCurrentUser();

        // Validar que sea estudiante
        if (user.getTipo() != User.UserType.STUDENT) {
            throw new BadRequestException("Solo estudiantes pueden sincronizar con TECSUP");
        }

        // Validar token
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new BadRequestException("Token TECSUP inválido");
        }

        // Guardar token si no existe
        if (user.getTecsupToken() == null || !user.getTecsupToken().equals(request.getToken())) {
            user.setTecsupToken(request.getToken().trim());
            userRepository.save(user);
        }

        // TODO: Llamar a API TECSUP real
        // Por ahora simulamos datos
        List<Event> syncedEvents = simulateTecsupApiCall(user, request.getToken());

        // Eliminar eventos anteriores sincronizados
        eventRepository.deleteByUserIdAndSincronizadoTecsup(user.getId(), true);

        // Guardar nuevos eventos
        List<Event> savedEvents = eventRepository.saveAll(syncedEvents);

        List<EventResponse> responses = savedEvents.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());

        return SyncResponse.builder()
                .eventosSincronizados(savedEvents.size())
                .eventos(responses)
                .build();
    }

    // SIMULACIÓN - Reemplazar con llamada real a API TECSUP
    private List<Event> simulateTecsupApiCall(User user, String token) {
        log.info("Simulando sincronización TECSUP para usuario: {}", user.getEmail());

        List<Event> events = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Simular 5 clases
        events.add(Event.builder()
                .user(user)
                .titulo("Programación Web")
                .fecha(today.plusDays(1))
                .hora(LocalTime.of(8, 0))
                .categoria(Event.EventCategory.CLASE)
                .curso("Desarrollo de Software")
                .descripcion("Clase sincronizada desde TECSUP")
                .sincronizadoTecsup(true)
                .build());

        events.add(Event.builder()
                .user(user)
                .titulo("Base de Datos")
                .fecha(today.plusDays(2))
                .hora(LocalTime.of(10, 0))
                .categoria(Event.EventCategory.CLASE)
                .curso("Gestión de Datos")
                .descripcion("Clase sincronizada desde TECSUP")
                .sincronizadoTecsup(true)
                .build());

        // Simular examen
        events.add(Event.builder()
                .user(user)
                .titulo("Examen Parcial - Spring Boot")
                .fecha(today.plusDays(7))
                .hora(LocalTime.of(14, 0))
                .categoria(Event.EventCategory.EXAMEN)
                .curso("Desarrollo de Software")
                .descripcion("Examen sincronizado desde TECSUP")
                .sincronizadoTecsup(true)
                .build());

        return events;
    }

    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .userId(event.getUser().getId())
                .titulo(event.getTitulo())
                .fecha(event.getFecha())
                .hora(event.getHora())
                .categoria(event.getCategoria())
                .descripcion(event.getDescripcion())
                .curso(event.getCurso())
                .sincronizadoTecsup(event.getSincronizadoTecsup())
                .createdAt(event.getCreatedAt())
                .build();
    }
}