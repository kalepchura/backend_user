// ============================================
// SyncController.java - Sincronización TECSUP
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.SyncTecsupRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.SyncResponse;
import com.tecsup.productivity.service.TecsupSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
    @RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final TecsupSyncService tecsupSyncService;

    /**
     * ✅ Habilitar sincronización TECSUP
     * POST /api/sync/tecsup/enable
     */
    @PostMapping("/tecsup/enable")
    public ResponseEntity<ApiResponse<SyncResponse>> enableSync(
            @Valid @RequestBody SyncTecsupRequest request
    ) {
        log.info("[SYNC] Habilitando sincronización TECSUP");
        SyncResponse response = tecsupSyncService.enableSync(request);
        return ResponseEntity.ok(
                ApiResponse.success("Sincronización TECSUP habilitada", response)
        );
    }

    /**
     * ✅ Deshabilitar sincronización TECSUP
     * POST /api/sync/tecsup/disable
     */
    @PostMapping("/tecsup/disable")
    public ResponseEntity<ApiResponse<Void>> disableSync() {
        log.info("[SYNC] Deshabilitando sincronización TECSUP");
        tecsupSyncService.disableSync();
        return ResponseEntity.ok(
                ApiResponse.success("Sincronización TECSUP deshabilitada. Datos locales preservados.", null)
        );
    }

    /**
     * ✅ Re-sincronizar datos TECSUP
     * POST /api/sync/tecsup/refresh
     */
    @PostMapping("/tecsup/refresh")
    public ResponseEntity<ApiResponse<SyncResponse>> refreshSync() {
        log.info("[SYNC] Re-sincronizando datos TECSUP");
        SyncResponse response = tecsupSyncService.refreshSync();
        return ResponseEntity.ok(
                ApiResponse.success("Datos TECSUP actualizados", response)
        );
    }

    /**
     * ⚠️ DEPRECADO - Mantener por compatibilidad
     * Usar /enable en su lugar
     */
    @Deprecated
    @PostMapping("/tecsup")
    public ResponseEntity<ApiResponse<SyncResponse>> syncTecsup(
            @Valid @RequestBody SyncTecsupRequest request
    ) {
        log.warn("[SYNC] Endpoint /sync/tecsup está deprecado, usa /sync/tecsup/enable");
        SyncResponse response = tecsupSyncService.enableSync(request);
        return ResponseEntity.ok(
                ApiResponse.success("Sincronización completada", response)
        );
    }
}