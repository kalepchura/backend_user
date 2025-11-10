// ============================================
// UserResponse.java
// ============================================
package com.tecsup.productivity.dto.response;

import com.tecsup.productivity.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private User.UserType tipo;
    private String tecsupToken;
    private Map<String, Object> preferences;
    private LocalDateTime createdAt;
}