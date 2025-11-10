// ============================================
// EventController.java - EP-03
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.CreateEventRequest;
import com.tecsup.productivity.dto.request.UpdateEventRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.EventResponse;
import com.tecsup.productivity.entity.Event;
import com.tecsup.productivity.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Event.EventCategory categoria
    ) {
        List<EventResponse> response = eventService.getEvents(fecha, categoria);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        EventResponse response = eventService.getEvent(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request
    ) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evento creado exitosamente", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        EventResponse response = eventService.updateEvent(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Evento actualizado exitosamente", response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(
                ApiResponse.success("Evento eliminado exitosamente", null)
        );
    }
}
