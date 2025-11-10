// ============================================
// AuthService.java - EP-01 (HU-1, HU-2)
// ============================================
package com.tecsup.productivity.service;

import com.tecsup.productivity.dto.request.LoginRequest;
import com.tecsup.productivity.dto.request.RegisterRequest;
import com.tecsup.productivity.dto.response.AuthResponse;
import com.tecsup.productivity.dto.response.UserResponse;
import com.tecsup.productivity.entity.User;
import com.tecsup.productivity.exception.BadRequestException;
import com.tecsup.productivity.repository.UserRepository;
import com.tecsup.productivity.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validar email único
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El email ya está registrado");
        }

        // Validar términos
        if (!request.getAcceptTerms()) {
            throw new BadRequestException("Debe aceptar los términos y condiciones");
        }

        // Preferencias por defecto (CA-02)
        Map<String, Object> defaultPreferences = new HashMap<>();
        defaultPreferences.put("chatEnabled", true);
        defaultPreferences.put("darkMode", false);
        defaultPreferences.put("notifications", true);

        // Crear usuario
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName().trim())
                .tipo(request.getTipo())
                .preferences(defaultPreferences)
                .build();

        user = userRepository.save(user);

        // Generar JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Autenticar
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        // Obtener usuario
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));

        // Generar JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
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