// ============================================
// UpdatePreferencesRequest.java - HU-2, CA-02
// ============================================
package com.tecsup.productivity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {
    private Boolean chatEnabled;
    private Boolean darkMode;
    private Boolean notifications;
}