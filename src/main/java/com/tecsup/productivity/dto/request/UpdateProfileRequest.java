// ============================================
// UpdateProfileRequest.java - HU-3, CA-03
// ============================================
package com.tecsup.productivity.dto.request;

import com.tecsup.productivity.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String name;
    private String email;
    private User.UserType tipo;
    private String tecsupToken;
}
