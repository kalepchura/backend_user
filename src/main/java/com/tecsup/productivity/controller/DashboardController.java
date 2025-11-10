// ============================================
// DashboardController.java - EP-02
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.DashboardResponse;
import com.tecsup.productivity.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse response = dashboardService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}