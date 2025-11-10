// ============================================
// SyncController.java - EP-03 (Sincronización TECSUP)
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.SyncTecsupRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.SyncResponse;
import com.tecsup.productivity.service.TecsupSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final TecsupSyncService tecsupSyncService;

    @PostMapping("/tecsup")
    public ResponseEntity<ApiResponse<SyncResponse>> syncTecsup(
            @Valid @RequestBody SyncTecsupRequest request
    ) {
        SyncResponse response = tecsupSyncService.syncTecsup(request);
        return ResponseEntity.ok(
                ApiResponse.success("Sincronización completada", response)
        );
    }
}