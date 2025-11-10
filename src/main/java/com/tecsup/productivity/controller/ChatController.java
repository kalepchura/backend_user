// ============================================
// ChatController.java - EP-05
// ============================================
package com.tecsup.productivity.controller;

import com.tecsup.productivity.dto.request.ChatMessageRequest;
import com.tecsup.productivity.dto.response.ApiResponse;
import com.tecsup.productivity.dto.response.ChatMessageResponse;
import com.tecsup.productivity.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @Valid @RequestBody ChatMessageRequest request
    ) {
        ChatMessageResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getChatHistory(
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        List<ChatMessageResponse> response = chatService.getChatHistory(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}