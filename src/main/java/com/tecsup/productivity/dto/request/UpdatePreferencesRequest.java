package com.tecsup.productivity.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {
    private Boolean chatEnabled;
    private Boolean darkMode;
    private Boolean notifications;
}