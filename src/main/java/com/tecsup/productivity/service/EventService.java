// ============================================
// EventService.java - VERSIÓN FINAL CON CAMPO CURSO
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CreateEventRequest;
import com.tecsup.productivity.dto.request.UpdateEventRequest;
import com.tecsup.productivity.dto.response.EventResponse;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.exception.ResourceNotFoundException;
import com.tecsup.productivity.exception.UnauthorizedException;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<EventResponse> getEvents(LocalDate fecha, Event.EventCategory categoria) {
        User user = securityUtil.getCurrentUser();
        List<Event> events;

        if (fecha != null && categoria != null) {
            events = eventRepository.findByUserIdAndFechaOrderByHoraAsc(user.getId(), fecha)
                    .stream()
                    .filter(e -> e.getCategoria().equals(categoria))
                    .collect(Collectors.toList());
        } else if (fecha != null) {
            events = eventRepository.findByUserIdAndFechaOrderByHoraAsc(user.getId(), fecha);
        } else if (categoria != null) {
            events = eventRepository.findByUserIdAndCategoriaOrderByFechaAscHoraAsc(user.getId(), categoria);
        } else {
            events = eventRepository.findByUserIdOrderByFechaAscHoraAsc(user.getId());
        }

        return events.stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        Event event = findEventById(id);
        validateOwnership(event);
        return mapToEventResponse(event);
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        User user = securityUtil.getCurrentUser();

        // ✅ Construir evento con curso OPCIONAL
        Event event = Event.builder()
                .user(user)
                .titulo(request.getTitulo().trim())
                .fecha(request.getFecha())
                .hora(request.getHora())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .curso(request.getCurso() != null && !request.getCurso().isBlank()
                        ? request.getCurso().trim()
                        : null) // ✅ NULL si no se envía
                .source("user")
                .sincronizadoTecsup(false)
                .build();

        event = eventRepository.save(event);
        log.info("[EVENT] Evento creado manualmente: {} por usuario {}",
                event.getId(), user.getId());

        return mapToEventResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        Event event = findEventById(id);
        validateOwnership(event);

        // ✅ Validar que no es evento sincronizado de TECSUP
        if ("tecsup".equals(event.getSource())) {
            throw new BadRequestException(
                    "No puedes editar eventos sincronizados desde TECSUP. " +
                            "Desactiva la sincronización o edita en Canvas."
            );
        }

        if (request.getTitulo() != null && !request.getTitulo().isBlank()) {
            event.setTitulo(request.getTitulo().trim());
        }

        if (request.getFecha() != null) {
            event.setFecha(request.getFecha());
        }

        if (request.getHora() != null) {
            event.setHora(request.getHora());
        }

        if (request.getCategoria() != null) {
            event.setCategoria(request.getCategoria());
        }

        if (request.getDescripcion() != null) {
            event.setDescripcion(request.getDescripcion().trim());
        }

        // ✅ Actualizar curso si viene en el request
        if (request.getCurso() != null) {
            event.setCurso(request.getCurso().isBlank()
                    ? null
                    : request.getCurso().trim());
        }

        event = eventRepository.save(event);
        return mapToEventResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = findEventById(id);
        validateOwnership(event);

        // ✅ Validar que no es evento sincronizado de TECSUP
        if ("tecsup".equals(event.getSource())) {
            throw new BadRequestException(
                    "No puedes eliminar eventos sincronizados desde TECSUP. " +
                            "Desactiva la sincronización o elimina en Canvas."
            );
        }

        eventRepository.delete(event);
        log.info("[EVENT] Evento eliminado: {} por usuario {}",
                id, securityUtil.getCurrentUserId());
    }

    private Event findEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
    }

    private void validateOwnership(Event event) {
        Long currentUserId = securityUtil.getCurrentUserId();
        if (!event.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedException("No tienes permiso para acceder a este evento");
        }
    }

    private EventResponse mapToEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .titulo(event.getTitulo())
                .fecha(event.getFecha())
                .hora(event.getHora())
                .categoria(event.getCategoria())
                .descripcion(event.getDescripcion())
                .curso(event.getCurso()) // ✅ Puede ser NULL
                .source(event.getSource())
                .tecsupExternalId(event.getTecsupExternalId())
                .sincronizadoTecsup(event.getSincronizadoTecsup())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}