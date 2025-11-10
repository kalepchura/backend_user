// ============================================
// UserService.java - EP-01 (HU-3)
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.UpdatePreferencesRequest;
import com.tecsup.productivity.dto.request.UpdateProfileRequest;
import com.tecsup.productivity.dto.response.UserResponse;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        User user = securityUtil.getCurrentUser();
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = securityUtil.getCurrentUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equalsIgnoreCase(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("El email ya está en uso");
            }
            user.setEmail(request.getEmail().toLowerCase().trim());
        }

        if (request.getTipo() != null) {
            user.setTipo(request.getTipo());
        }

        if (request.getTecsupToken() != null) {
            user.setTecsupToken(request.getTecsupToken().trim());
        }

        user = userRepository.save(user);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updatePreferences(UpdatePreferencesRequest request) {
        User user = securityUtil.getCurrentUser();
        Map<String, Object> preferences = user.getPreferences();

        if (request.getChatEnabled() != null) {
            preferences.put("chatEnabled", request.getChatEnabled());
        }

        if (request.getDarkMode() != null) {
            preferences.put("darkMode", request.getDarkMode());
        }

        if (request.getNotifications() != null) {
            preferences.put("notifications", request.getNotifications());
        }

        user.setPreferences(preferences);
        user = userRepository.save(user);

        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .tipo(user.getTipo())
                .tecsupToken(user.getTecsupToken())
                .preferences(user.getPreferences())
                .createdAt(user.getCreatedAt())
                .build();
    }
}