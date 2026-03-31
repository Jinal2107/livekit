package com.livekitdemo.livekitdemo.controller;

import com.livekitdemo.livekitdemo.service.LiveKitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Backward-compatible token endpoint.
 * New clients should use POST /api/rooms/join instead.
 */
@RestController
@RequestMapping("/livekit")
@RequiredArgsConstructor
@CrossOrigin
public class LiveKitController {

    private final LiveKitService liveKitService;

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> getToken(
            @RequestParam String room,
            @RequestParam String user
    ) {
        String token = liveKitService.generateToken(room, user);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
