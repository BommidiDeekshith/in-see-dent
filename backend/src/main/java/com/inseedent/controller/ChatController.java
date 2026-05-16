package com.inseedent.controller;

import com.inseedent.dto.ChatRequest;
import com.inseedent.dto.ChatResponse;
import com.inseedent.service.chat.ChatService;
import com.inseedent.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Chat Assistant", description = "Investigation chat assistant")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "Send message to investigation assistant")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.success(chatService.chat(request)));
    }
}
