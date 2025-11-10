// ============================================
// EventService.java - EP-03 (HU-6, CA-09)
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.CreateEventRequest;
import com.tecsup.productivity.dto.request.UpdateEventRequest;
import com.tecsup.productivity.dto.response.EventResponse;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.ResourceNotFoundException;
import com.tecsup.productivity.exception.UnauthorizedException;
import com.tecsup.productivity.repository.EventRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

        Event event = Event.builder()
                .user(user)
                .titulo(request.getTitulo().trim())
                .fecha(request.getFecha())
                .hora(request.getHora())
                .categoria(request.getCategoria())
                .descripcion(request.getDescripcion())
                .sincronizadoTecsup(false)
                .build();

        event = eventRepository.save(event);
        return mapToEventResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        Event event = findEventById(id);
        validateOwnership(event);

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

        event = eventRepository.save(event);
        return mapToEventResponse(event);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = findEventById(id);
        validateOwnership(event);
        eventRepository.delete(event);
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
